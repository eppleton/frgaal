import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileBuilder;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.FieldBuilder;
import java.lang.classfile.FieldElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.FieldTransform;
import java.lang.classfile.MethodBuilder;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InjectFutureAnnotations {

    public static void main(String[] args) throws IOException {
        Path ctSymSource = Paths.get(args[0]);
        Path ctSymTarget = Paths.get(args[1]);
        Map<String, Map<Integer, Status>> element2Status = new HashMap<>();
        int maxVersion = 8;
        for (int version = 8; version <= Character.MAX_RADIX; version++) {
            int versionFin = version;
            char versionKey = Character.toUpperCase(Integer.toString(version, Character.MAX_RADIX).charAt(0));
            boolean exists = false;
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(ctSymSource)) {
                for (Path versionDir : ds) {
                    if (versionDir.getFileName().toString().contains("" + versionKey)) {
                        Files.walk(versionDir)
                             .filter(p -> Files.isRegularFile(p))
                             .forEach(p -> analyzeClass(p, element2Status, versionFin));
                        exists = true;
                    }
                }
            }
            if (!exists) {
                break;
            }
            maxVersion = version;
        }
        int maxVersionFin = maxVersion;
        for (int version = 8; version <= Character.MAX_RADIX; version++) {
            int versionFin = version;
            char versionKey = Character.toUpperCase(Integer.toString(version, Character.MAX_RADIX).charAt(0));
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(ctSymSource)) {
                for (Path versionDir : ds) {
                    if (versionDir.getFileName().toString().contains("" + versionKey)) {
                        Files.walk(versionDir)
                             .filter(p -> Files.isRegularFile(p))
                             .forEach(p -> augmentClass(p, ctSymTarget.resolve(ctSymSource.relativize(p)), element2Status, versionFin, maxVersionFin));
                    }
                }
            }
        }
    }
    
    private static void analyzeClass(Path classfile, Map<String, Map<Integer, Status>> element2Status, int version) {
        try {
            ClassModel model = ClassFile.of().parse(classfile);
            String thisClass = keyFor(model);
            element2Status.computeIfAbsent(thisClass, _ -> new HashMap<>()).put(version, getStatus(model));
            model.methods().forEach(m -> {
                element2Status.computeIfAbsent(keyFor(m), _ -> new HashMap<>()).put(version, getStatus(m));
            });
            model.fields().forEach(f -> {
                element2Status.computeIfAbsent(keyFor(f), _ -> new HashMap<>()).put(version, getStatus(f));
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static Status getStatus(AttributedElement el) {
        //XXX: should check the RuntimeVisible annotations:
        return el.findAttribute(DeprecatedAttribute.of().attributeMapper()).isPresent() ? Status.EXISTS_DEPRECATED : Status.EXISTS_NON_DEPRECATED;
    }

    private static String keyFor(Object o) {
        return switch (o) {
            case ClassModel m -> m.thisClass().asInternalName();
            case MethodModel m -> m.parent().map(InjectFutureAnnotations::keyFor).orElse("") + ":" + m.methodName().stringValue() + ":" + m.methodType().toString();
            case FieldModel m -> m.parent().map(InjectFutureAnnotations::keyFor).orElse("") + ":" + m.fieldName().stringValue() + ":" + m.fieldType().toString();
            default -> throw new IllegalStateException();
        };
    }

    private static void augmentClass(Path source, Path target, Map<String, Map<Integer, Status>> element2Status, int version, int maxVersion) {
        try {
            ClassModel clazz = ClassFile.of().parse(source);
            byte[] newData = ClassFile.of().transformClass(clazz, new ClassTransform() {
                private boolean seenAnnotations;
                @Override
                public void accept(ClassBuilder builder, ClassElement element) {
                    if (element instanceof RuntimeInvisibleAnnotationsAttribute attr) {
                        seenAnnotations = true;
                        checkAnnotations(keyFor(clazz), element2Status, version, maxVersion, builder, attr);
                    } else if (element instanceof MethodModel method) {
                        builder.transformMethod(method, new MethodTransform() {
                            private boolean seenAnnotations;
                            @Override
                            public void accept(MethodBuilder builder, MethodElement element) {
                                if (element instanceof RuntimeInvisibleAnnotationsAttribute attr) {
                                    seenAnnotations = true;
                                    checkAnnotations(keyFor(method), element2Status, version, maxVersion, builder, attr);
                                } else {
                                    builder.accept(element);
                                }
                            }
                            @Override
                            public void atEnd(MethodBuilder builder) {
                                if (!seenAnnotations) {
                                    checkAnnotations(keyFor(method), element2Status, version, maxVersion, builder, null);
                                }
                            }
                        });
                    } else if (element instanceof FieldModel field) {
                        builder.transformField(field, new FieldTransform() {
                            private boolean seenAnnotations;
                            @Override
                            public void accept(FieldBuilder builder, FieldElement element) {
                                if (element instanceof RuntimeInvisibleAnnotationsAttribute attr) {
                                    seenAnnotations = true;
                                    checkAnnotations(keyFor(field), element2Status, version, maxVersion, builder, attr);
                                } else {
                                    builder.accept(element);
                                }
                            }
                            @Override
                            public void atEnd(FieldBuilder builder) {
                                if (!seenAnnotations) {
                                    checkAnnotations(keyFor(field), element2Status, version, maxVersion, builder, null);
                                }
                            }
                        });
                    } else {
                        builder.accept(element);
                    }
                }
                @Override
                public void atEnd(ClassBuilder builder) {
                    if (!seenAnnotations) {
                        checkAnnotations(keyFor(clazz), element2Status, version, maxVersion, builder, null);
                    }
                }
            });
//            if (!Arrays.equals(newData, Files.readAllBytes(source))) {
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(target)) {
                    out.write(newData);
                }
//            }
//            String thisClass = model.thisClass().asInternalName();
//            element2Status.computeIfAbsent(thisClass, _ -> new HashMap<>()).put(versionNumber, getStatus(model));
//            model.methods().forEach(m -> {
//                element2Status.computeIfAbsent(thisClass + ":" + m.methodName().stringValue() + ":" + m.methodType().toString(), _ -> new HashMap<>()).put(versionNumber, getStatus(m));
//            });
//            model.fields().forEach(f -> {
//                element2Status.computeIfAbsent(thisClass + ":" + f.fieldName().stringValue() + ":" + f.fieldType().toString(), _ -> new HashMap<>()).put(versionNumber, getStatus(f));
//            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void checkAnnotations(String element, Map<String, Map<Integer, Status>> element2Status, int version, int maxVersion, ClassFileBuilder builder, RuntimeInvisibleAnnotationsAttribute attr) {
//        if (element.contains("ReferenceQueue")) {
//            System.err.println("versionNumber: " + versionNumber);
//        }
        List<Annotation> annotations = new ArrayList<>();

        for (int v = version; v < maxVersion; v++) {
            if (!element2Status.get(element).containsKey(v)) {
//                if (element.contains("ReferenceQueue")) {
//                    System.err.println("removed in v: " + v);
//                }
                annotations.add(Annotation.of(ClassDesc.of("frgaal.internal.Future+Removed+Annotation"), AnnotationElement.of("value", AnnotationValue.of(v))));
                break;
            }
        }
        if (element2Status.get(element).get(version) == Status.EXISTS_NON_DEPRECATED) {
            for (int v = version; v < maxVersion; v++) {
                if (element2Status.get(element).get(v) == Status.EXISTS_DEPRECATED) {
                    annotations.add(Annotation.of(ClassDesc.of("frgaal.internal.Future+Deprecated+Annotation"), AnnotationElement.of("value", AnnotationValue.of(v))));
                    break;
                }
            }
        }
        
        if (annotations.isEmpty()) {
            if (attr != null) {
                builder.accept(attr);
            }

            return ;
        }
        if (attr != null) {
            annotations.addAll(attr.annotations());
        }
        builder.accept(RuntimeInvisibleAnnotationsAttribute.of(annotations));
    }

//    private static final class Visitor<
//        C extends ClassFileTransform<C, E, B>,
//        E extends ClassFileElement,
//        B extends ClassFileBuilder<E, B>> implements ClassFileTransform<C, E, B> {
//        
//    }
//ClassFileTransform<ClassTransform, ClassElement, ClassBuilder>
    private enum Status {
        EXISTS_NON_DEPRECATED,
        EXISTS_DEPRECATED;
    }
}
