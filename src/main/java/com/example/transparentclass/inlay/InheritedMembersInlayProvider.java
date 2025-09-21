package com.example.transparentclass.inlay;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class InheritedMembersInlayProvider implements InlayHintsProvider<NoSettings> {

    @NotNull
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file, @NotNull Editor editor, @NotNull NoSettings settings, @NotNull InlayHintsSink sink) {
        return (element, editor1, sink1) -> {
            boolean isFieldExist =  false;
            boolean isMethodExist =  false;



            if (element instanceof PsiClass psiClass) {
                PsiElement lBraceElement = psiClass.getLBrace();
                if (!(lBraceElement instanceof PsiJavaToken)) return true;

                PresentationFactory factory = new PresentationFactory(editor1);
                PsiClass superClass = psiClass.getSuperClass();
                if (superClass == null || "Object".equals(superClass.getName())) return true;


                tpcPrint(factory.text("Inherited Fields"), sink1, lBraceElement);

                //fields
                Set<String> currentFieldNames = Arrays.stream(psiClass.getFields()).map(PsiField::getName).collect(Collectors.toSet());
                if(!currentFieldNames.isEmpty()){
                    isFieldExist = true;
                }
                for (PsiField field : superClass.getFields()) {
                    if (!field.getModifierList().hasModifierProperty(PsiModifier.PRIVATE) && !currentFieldNames.contains(field.getName())) {
                        String hintText;
                        if(field.hasModifierProperty(PsiModifier.PROTECTED)) {
                            hintText = String.format("%s %s %s", "protected", field.getType().getPresentableText(), field.getName());
                        }else{
                            hintText = String.format("%s %s %s", "public", field.getType().getPresentableText(), field.getName());
                        }
                        tpcPrint(createClickablePresentation(factory, hintText, field), sink1, lBraceElement);
                    }
                }

                tpcPrint(factory.text(" "), sink1, lBraceElement);
                tpcPrint(factory.text("Inherited Methods "), sink1, lBraceElement);


                // method
                Set<String> currentMethodNames = Arrays.stream(psiClass.getMethods()).map(PsiMethod::getName).collect(Collectors.toSet());
                Set<String> processedSignatures = new HashSet<>();
                for (PsiMethod method : superClass.getMethods()) {
                    if (!method.getModifierList().hasModifierProperty(PsiModifier.PRIVATE) && !method.isConstructor() && !currentMethodNames.contains(method.getName())) {
                        String signature = getMethodSignature(method);
                        String hintText;
                        if (processedSignatures.add(signature)) {
                            if(method.hasModifierProperty(PsiModifier.PROTECTED)){
                                hintText =  "protected " + buildHintText(method);
                            }else{
                                hintText = "public  " + buildHintText(method);
                            }
                            tpcPrint(createClickablePresentation(factory, hintText, method), sink1, lBraceElement);
                        }
                    }
                }
            }
            return true;
        };
    }

    private static void tpcPrint(InlayPresentation factory, InlayHintsSink sink1, PsiElement lBraceElement) {
        InlayPresentation spacer = factory;
        sink1.addBlockElement(lBraceElement.getTextOffset() + 1, true, true, 0, spacer);
    }

    private InlayPresentation createClickablePresentation(PresentationFactory factory, String text, PsiElement element) {
        InlayPresentation textPresentation = factory.text(text);

        InlayPresentation finalPresentation = factory.roundWithBackground(textPresentation);

        return factory.referenceOnHover(finalPresentation, (mouseEvent, point) -> {
            if (element instanceof Navigatable) {
                ((Navigatable) element).navigate(true);
            }
        });
    }

    private String buildHintText(PsiMethod method) {
        String returnType = method.getReturnType() != null ? method.getReturnType().getPresentableText() : "void";
        String params = Arrays.stream(method.getParameterList().getParameters()).map(p -> p.getType().getPresentableText()).collect(Collectors.joining(", "));
        return String.format("%s %s(%s)", returnType, method.getName(), params);
    }
    private String getMethodSignature(PsiMethod method) {
        String params = Arrays.stream(method.getParameterList().getParameters()).map(p -> p.getType().getCanonicalText()).collect(Collectors.joining(","));
        return method.getName() + "(" + params + ")";
    }

    @NotNull @Override public String getName() { return "Inherited Members"; }
    @NotNull @Override public SettingsKey<NoSettings> getKey() { return new SettingsKey<>("java.inherited.members"); }
    @NotNull @Override public NoSettings createSettings() { return new NoSettings(); }
    @Nullable @Override public String getPreviewText() { return null; }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener listener) {
                return new JPanel();
            }
        };
    }
}