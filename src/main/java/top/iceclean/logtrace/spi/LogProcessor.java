package top.iceclean.logtrace.spi;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import top.iceclean.logtrace.annotation.EnableLogTrace;
import lombok.SneakyThrows;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.FileWriter;
import java.util.Set;

/**
 * @author : Ice'Clean
 * @date : 2022-04-19
 *
 * 为 @EnableLogTrace 配置注解处理器，自动生成 logTrace 变量
 */
@SupportedAnnotationTypes("top.iceclean.logtrace.annotation.EnableLogTrace")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LogProcessor extends AbstractProcessor {

    private JavacTrees javacTrees;
    private TreeMaker treeMaker;
    private Names names;
    private Messager messager;
    private FileWriter fileWriter;

    @SneakyThrows
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        javacTrees = JavacTrees.instance(processingEnv);
        treeMaker = TreeMaker.instance(context);
        names = Names.instance(context);
    }

    @SneakyThrows
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 获取注解类的集合，之后依次去处理
        Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(EnableLogTrace.class);
        for (Element element : set) {
            // 获取当前类的抽象语法树
            JCTree tree = javacTrees.getTree(element);
            // 获取抽象语法树的所有节点
            // Visitor 抽象内部类，内部定义了访问各种语法节点的方法
            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    jcClassDecl.defs.stream()
                            .filter(element -> element.getKind().equals(Tree.Kind.METHOD))
                            .map(methodTree -> (JCTree.JCMethodDecl) methodTree)
                            .forEach(methodTree -> {
                                // 添加从本地线程获取自定义日志的语句
                                List<JCTree.JCStatement> stats = methodTree.body.stats;

                                // 在构造方法中，有 super 语句的话获取日志需要放到第二句
                                if (methodTree.getName().toString().contains("<init>") && stats.size() > 0 && stats.get(0).toString().contains("super")) {
                                    stats.tail.prepend(createGetLogTraceStatement());
                                } else {
                                    stats = stats.prepend(createGetLogTraceStatement());
                                }

                                methodTree.body.stats = stats;
                            });
                    super.visitClassDef(jcClassDecl);
                }
            });
        }
        return true;
    }


    /**
     * 创建一个变量并初始化
     * @param modifiers 修改器
     * @param type 变量类型（全类名）
     * @param name 变量名称
     * @param init 变量初始化语句
     * @return 创建变量并赋值的语句
     */
    private JCTree.JCVariableDecl makeVarDef(JCTree.JCModifiers modifiers, String type, String name, JCTree.JCExpression init) {
        return treeMaker.VarDef(
                modifiers,
                // 变量名字
                names.fromString(name),
                // 变量类型
                memberAccess(type),
                // 初始化语句
                init
        );
    }

    public JCTree.JCStatement createGetLogTraceStatement() {
        // LogTrace logTrace = LogTrace.getLogTrace();
        return makeVarDef(
                treeMaker.Modifiers(0),
                "top.iceclean.logtrace.bean.LogTrace",
                "logTrace",
                treeMaker.Apply(
                        List.nil(),
                        treeMaker.Select(
                                memberAccess("top.iceclean.logtrace.bean.LogTrace"),
                                names.fromString("getLogTrace")
                        ),
                        List.nil()
                )
        );
//        return treeMaker.Exec(
//                treeMaker.Assign(
//                        treeMaker.Ident(names.fromString("logTrace")),
//                        treeMaker.Apply(
//                                List.nil(),
//                                treeMaker.Select(
//                                        memberAccess("top.iceclean.logtrace.bean.LogTrace"),
//                                        names.fromString("getLogTrace")
//                                ),
//                                List.nil()
//                        )
//                )
//        );
    }

    /**
     * 创建 域/方法 的多级访问, 方法的标识只能是最后一个
     */
    private JCTree.JCExpression memberAccess(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(names.fromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, names.fromString(componentArray[i]));
        }
        return expr;
    }

}
