package org.frgaal.tests.annotations.annotations;

import java.util.Arrays;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("org.frgaal.tests.annotations.annotations.*")
public class AP extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element el : roundEnv.getElementsAnnotatedWith(Ann.class)) {
            Ann ann = el.getAnnotation(Ann.class);
            System.out.println("RESULT:" + ann.v1());
            System.out.println("RESULT:" + ann.v2());
            System.out.println("RESULT:" + Arrays.toString(ann.v3()[0].value()));
            System.out.println("RESULT:" + Arrays.toString(ann.v3()[1].value()));
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
    
}
