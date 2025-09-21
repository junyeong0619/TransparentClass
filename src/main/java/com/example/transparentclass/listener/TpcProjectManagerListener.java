package com.example.transparentclass.listener;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.ui.Messages;

public class TpcProjectManagerListener implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("TransparentClass Welcome")
                .createNotification("TransparentClass plugin is active!", NotificationType.INFORMATION)
                .notify(project);
    }
}
