package com.foronly.example.annotation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

/**
 * <p>
 * {@link www.baeldung.com/java-annotation-processing-builder} 注解处理器的实现
 * </p>
 *
 * @author li_cang_long
 * @since 2023/9/5 23:54
 */
@SupportedAnnotationTypes("com.foronly.example.annotation.BuilderProperty")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class BuilderProcessor extends AbstractProcessor {

	/**
	 * 使用RoundEnvironment实例来接收使用@BuilderProperty注释进行注释的所有元素
	 *
	 * @param annotations the annotation types requested to be processed
	 * @param roundEnv    environment for information about the current and prior round
	 * @return
	 */
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (TypeElement annotation : annotations) {

			Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
			// 使用Collectors.partitioningBy()收集器将带注释的方法分成两个集合：正确注释的 setter 和其他错误注释的方法
			// 使用Element.asType()方法来接收TypeMirror类的实例，这使我们能够内省类型，即使我们仅处于源处理阶段
			Map<Boolean, List<Element>> annotatedMethods = annotatedElements.stream()
																			.collect(Collectors.partitioningBy(element -> ((ExecutableType) element.asType())
																					.getParameterTypes()
																					.size() == 1 && element
																					.getSimpleName().toString()
																					.startsWith("set")));

			List<Element> setters      = annotatedMethods.get(true);
			List<Element> otherMethods = annotatedMethods.get(false);

			otherMethods.forEach(element -> processingEnv.getMessager()
														 .printMessage(Diagnostic.Kind.ERROR, "@BuilderProperty must be applied to a setXxx method with a single argument", element));
			// 如果正确的 setters 集合为空，则没有必要继续当前类型元素集迭代：
			if (setters.isEmpty()) {
				continue;
			}
			// 如果 setters 集合至少有一个元素，我们将使用它从封闭元素中获取完全限定的类名，在 setter 方法的情况下，该类名似乎是源类本身
			String className = ((TypeElement) setters.get(0).getEnclosingElement()).getQualifiedName().toString();
			// 生成构建器类所需的最后一点信息是 setter 名称与其参数类型名称之间的映射
			Map<String, String> setterMap = setters.stream().collect(Collectors.toMap(setter -> setter.getSimpleName()
																									  .toString(), setter -> ((ExecutableType) setter.asType())
					.getParameterTypes().get(0).toString()));

			try {
				writeBuilderFile(className, setterMap);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return true;
	}

	private void writeBuilderFile(String className, Map<String, String> setterMap) throws IOException {

		String packageName = null;
		int    lastDot     = className.lastIndexOf('.');
		if (lastDot > 0) {
			packageName = className.substring(0, lastDot);
		}

		String simpleClassName        = className.substring(lastDot + 1);
		String builderClassName       = className + "Builder";
		String builderSimpleClassName = builderClassName.substring(lastDot + 1);
		// 有了生成构建器类所需的所有信息：源类的名称、其所有 setter 名称及其参数类型。
		// 为了生成输出文件，我们将使用AbstractProcessor.processingEnv受保护属性中的对象再次提供的Filer实例
		JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);
		try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

			if (packageName != null) {
				out.print("package ");
				out.print(packageName);
				out.println(";");
				out.println();
			}

			out.print("public class ");
			out.print(builderSimpleClassName);
			out.println(" {");
			out.println();

			out.print("    private ");
			out.print(simpleClassName);
			out.print(" object = new ");
			out.print(simpleClassName);
			out.println("();");
			out.println();

			out.print("    public ");
			out.print(simpleClassName);
			out.println(" build() {");
			out.println("        return object;");
			out.println("    }");
			out.println();

			setterMap.entrySet().forEach(setter -> {
				String methodName   = setter.getKey();
				String argumentType = setter.getValue();

				out.print("    public ");
				out.print(builderSimpleClassName);
				out.print(" ");
				out.print(methodName);

				out.print("(");

				out.print(argumentType);
				out.println(" value) {");
				out.print("        object.");
				out.print(methodName);
				out.println("(value);");
				out.println("        return this;");
				out.println("    }");
				out.println();
			});

			out.println("}");

		}
	}

}