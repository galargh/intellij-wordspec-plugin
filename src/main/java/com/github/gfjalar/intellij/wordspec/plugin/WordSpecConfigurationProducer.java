package com.github.gfjalar.intellij.wordspec.plugin;

import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class WordSpecConfigurationProducer extends LazyRunConfigurationProducer<ExternalSystemRunConfiguration> {
  private final static ProjectSystemId GRADLE = new ProjectSystemId("GRADLE");

  @Override
  protected boolean setupConfigurationFromContext(@NotNull ExternalSystemRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
    final Location contextLocation = context.getLocation();
    assert contextLocation != null;

    PsiElement element = contextLocation.getPsiElement();
    if (element == null) { return false; }

    //WordSpecExpression expression = WordSpecExpression.fromLeafIdentifier(element);
    WordSpecExpression expression = WordSpecExpression.fromLeafStringContent(element);
    if (expression == null) { return false; }

    final PsiClass containingClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    if (containingClass == null) { return false; }

    final Module module = context.getModule();
    if (module == null) return false;

    if (! ExternalSystemApiUtil.isExternalSystemAwareModule(GRADLE, module)) { return false; }

    final String projectPath = resolveProjectPath(module);
    if (projectPath == null) { return false; }

    final String classAndTest = containingClass.getQualifiedName() + "." + expression.toSentence();
    final String testFilter = "--tests \"" + classAndTest + "\"";

    final ExternalSystemTaskExecutionSettings settings = configuration.getSettings();

    settings.setExternalProjectPath(projectPath);
    settings.setScriptParameters(testFilter);

    configuration.setName(classAndTest);
    JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, contextLocation);
    return true;
  }

  @Override
  public void onFirstRun(@NotNull ConfigurationFromContext configuration, @NotNull ConfigurationContext context, @NotNull Runnable startRunnable) {
    final List<String> testTaskNames = findTestTaskNames(context.getModule());
    assert ! testTaskNames.isEmpty();

    ExternalSystemRunConfiguration runConfiguration = (ExternalSystemRunConfiguration) configuration.getConfiguration();

    if (testTaskNames.size() == 1) {
      String name = testTaskNames.get(0);
      runConfiguration.getSettings().setTaskNames(Arrays.asList("clean" + StringUtils.capitalize(name), name));
      startRunnable.run();
    } else {
      JBPopupFactory.getInstance()
        .createPopupChooserBuilder(testTaskNames)
        .setItemChosenCallback(it -> {
          runConfiguration.getSettings().setTaskNames(Arrays.asList("clean" + StringUtils.capitalize(it), it));
          startRunnable.run();
        })
        .createPopup()
        .showInBestPositionFor(context.getDataContext());
    }
  }

  @Override
  public boolean isConfigurationFromContext(@NotNull ExternalSystemRunConfiguration configuration, @NotNull ConfigurationContext context) {
    final Location contextLocation = context.getLocation();
    assert contextLocation != null;

    PsiElement element = contextLocation.getPsiElement();
    if (element == null) { return false; }

    //WordSpecExpression expression = WordSpecExpression.fromLeafIdentifier(element);
    WordSpecExpression expression = WordSpecExpression.fromLeafStringContent(element);
    if (expression == null) { return false; }

    final PsiClass containingClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    if (containingClass == null) { return false; }

    final Module module = context.getModule();
    if (module == null) return false;

    if (! ExternalSystemApiUtil.isExternalSystemAwareModule(GRADLE, module)) { return false; }

    final String projectPath = resolveProjectPath(module);
    if (projectPath == null) { return false; }

    if (! projectPath.equals(configuration.getSettings().getExternalProjectPath())) { return false; }

    final String classAndTest = containingClass.getQualifiedName() + "." + expression.toSentence();
    final String testFilter = "--tests \"" + classAndTest + "\"";

    return configuration.getName().equals(classAndTest) && configuration.getSettings().getScriptParameters().contains(testFilter);
  }

  private String resolveProjectPath(Module module) {
    final String rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
    final String projectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
    if (projectPath == null) { return rootProjectPath; }
    return projectPath;
  }

  private List<String> findTestTaskNames(Module module) {
    String externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
    ExternalProjectInfo externalProjectInfo = ExternalSystemUtil.getExternalProjectInfo(module.getProject(), GRADLE, externalProjectPath);
    DataNode<ModuleData> moduleDataDataNode = ExternalSystemApiUtil.find(externalProjectInfo.getExternalProjectStructure(), ProjectKeys.MODULE, it -> it.getData().getLinkedExternalProjectPath().equals(externalProjectPath));
    List<String> testTaskNames = ExternalSystemApiUtil.findAll(moduleDataDataNode, ProjectKeys.TASK).stream().filter(it -> it.getData().getType().equals("org.gradle.api.tasks.testing.Test")).map(it -> it.getData().getName()).collect(Collectors.toList());
    return testTaskNames;
  }

  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return ExternalSystemUtil.findConfigurationType(GRADLE).getFactory();
  }
}
