package com.example.transparentclass.inlay;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
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

    private static final SettingsKey<NoSettings> SETTINGS_KEY = new SettingsKey<>("java.inherited.members");

    @NotNull
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file, @NotNull Editor editor,
                                               @NotNull NoSettings settings, @NotNull InlayHintsSink sink) {
        return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
                if (element instanceof PsiClass psiClass) {
                    PsiElement lBraceElement = psiClass.getLBrace();
                    if (!(lBraceElement instanceof PsiJavaToken)) return true;

                    PsiClass superClass = psiClass.getSuperClass();
                    if (superClass == null || "java.lang.Object".equals(superClass.getQualifiedName())) {
                        return true;
                    }

                    boolean hasInheritedFields = showInheritedFields(psiClass, superClass, sink, lBraceElement);
                    showInheritedMethods(psiClass, superClass, sink, lBraceElement, hasInheritedFields);
                }
                return true;
            }

            private boolean showInheritedFields(PsiClass psiClass, PsiClass superClass,
                                                InlayHintsSink sink, PsiElement lBraceElement) {
                Set<String> currentFieldNames = Arrays.stream(psiClass.getFields())
                        .map(PsiField::getName)
                        .collect(Collectors.toSet());

                boolean hasFields = false;

                for (PsiField field : superClass.getFields()) {
                    if (shouldShowMember(field) && !currentFieldNames.contains(field.getName())) {
                        if (!hasFields) {
                            addInlayHint(sink, lBraceElement, "Inherited Fields");
                            hasFields = true;
                        }

                        String visibility = getVisibilityModifier(field);
                        String hintText = String.format("%s %s %s",
                                visibility,
                                field.getType().getPresentableText(),
                                field.getName());

                        addClickableInlayHint(sink, lBraceElement, hintText, field);
                    }
                }

                return hasFields;
            }

            private void showInheritedMethods(PsiClass psiClass, PsiClass superClass,
                                              InlayHintsSink sink, PsiElement lBraceElement, boolean hasInheritedFields) {
                Set<String> currentMethodSignatures = Arrays.stream(psiClass.getMethods())
                        .map(this::getMethodSignature)
                        .collect(Collectors.toSet());

                Set<String> processedSignatures = new HashSet<>();
                boolean hasMethod = false;

                for (PsiMethod method : superClass.getMethods()) {
                    if (shouldShowMember(method) && !method.isConstructor() &&
                            !currentMethodSignatures.contains(getMethodSignature(method))) {

                        String signature = getMethodSignature(method);
                        if (processedSignatures.add(signature)) {
                            if (!hasMethod) {
                                if (hasInheritedFields) {
                                    addInlayHint(sink, lBraceElement, " ");
                                }
                                addInlayHint(sink, lBraceElement, "Inherited Methods");
                                hasMethod = true;
                            }

                            String visibility = getVisibilityModifier(method);
                            String hintText = visibility + " " + buildHintText(method);
                            addClickableInlayHint(sink, lBraceElement, hintText, method);
                        }
                    }
                }
            }

            private void addInlayHint(InlayHintsSink sink, PsiElement element, String text) {
                InlayPresentation presentation = getFactory().text(text);
                sink.addBlockElement(element.getTextOffset() + 1, true, true, 0, presentation);
            }

            private void addClickableInlayHint(InlayHintsSink sink, PsiElement element, String text, PsiElement targetElement) {
                InlayPresentation textPresentation = getFactory().text(text);
                InlayPresentation backgroundPresentation = getFactory().roundWithBackground(textPresentation);

                InlayPresentation clickablePresentation = getFactory().referenceOnHover(backgroundPresentation,
                        (mouseEvent, point) -> {
                            if (targetElement instanceof Navigatable navigatable) {
                                navigatable.navigate(true);
                            }
                        });

                sink.addBlockElement(element.getTextOffset() + 1, true, true, 0, clickablePresentation);
            }

            private boolean shouldShowMember(PsiModifierListOwner member) {
                PsiModifierList modifierList = member.getModifierList();
                return modifierList != null && !modifierList.hasModifierProperty(PsiModifier.PRIVATE);
            }

            private String getVisibilityModifier(PsiModifierListOwner member) {
                if (member.hasModifierProperty(PsiModifier.PROTECTED)) {
                    return "protected";
                } else if (member.hasModifierProperty(PsiModifier.PUBLIC)) {
                    return "public";
                }
                return "package";
            }

            private String buildHintText(PsiMethod method) {
                String returnType = method.getReturnType() != null ?
                        method.getReturnType().getPresentableText() : "void";
                String params = Arrays.stream(method.getParameterList().getParameters())
                        .map(p -> p.getType().getPresentableText())
                        .collect(Collectors.joining(", "));
                return String.format("%s %s(%s)", returnType, method.getName(), params);
            }

            private String getMethodSignature(PsiMethod method) {
                String params = Arrays.stream(method.getParameterList().getParameters())
                        .map(p -> p.getType().getCanonicalText())
                        .collect(Collectors.joining(","));
                return method.getName() + "(" + params + ")";
            }
        };
    }

    @NotNull
    @Override
    public String getName() {
        return "Inherited Members";
    }

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return SETTINGS_KEY;
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return new NoSettings();
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return null;
    }

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