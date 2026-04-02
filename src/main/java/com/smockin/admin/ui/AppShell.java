package com.smockin.admin.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@PWA(name = "sMockin Admin", shortName = "sMockin", offlinePath = "offline.html", offlineResources = { "./images/offline.png"})
@Theme(themeClass = Lumo.class)
@Push
public class AppShell implements AppShellConfigurator {
}
