package com.smockin.admin.ui.views.http;

import com.smockin.admin.dto.RuleConditionDTO;
import com.smockin.admin.dto.RuleDTO;
import com.smockin.admin.dto.RuleGroupDTO;
import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import com.smockin.admin.ui.utils.UIFormattingUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import java.util.ArrayList;
import java.util.function.Consumer;

public class RuleDialog extends Dialog {

    private final RuleDTO rule;
    private final Consumer<RuleDTO> onSave;

    private IntegerField httpStatusCode = new IntegerField("HTTP Status");
    private ComboBox<String> responseContentType = new ComboBox<>("Content Type");
    private TextArea responseBody = new TextArea("Response Body");
    private IntegerField sleepInMillis = new IntegerField("Sleep (ms)");
    private IntegerField orderNo = new IntegerField("Order");

    private Binder<RuleDTO> binder = new Binder<>(RuleDTO.class);
    private Grid<RuleGroupDTO> groupsGrid = new Grid<>(RuleGroupDTO.class);

    public RuleDialog(RuleDTO rule, Consumer<RuleDTO> onSave) {
        this.rule = rule != null ? rule : new RuleDTO();
        this.onSave = onSave;
        if (this.rule.getGroups() == null) this.rule.setGroups(new ArrayList<>());

        setHeaderTitle(rule == null ? "New Rule" : "Edit Rule");

        configureForm();
        configureGroupsGrid();
        createButtons();

        binder.readBean(this.rule);
        groupsGrid.setItems(this.rule.getGroups());
    }

    private void configureForm() {
        responseContentType.setItems("application/json", "text/plain", "text/html", "application/xml");
        responseContentType.setAllowCustomValue(true);
        responseBody.setHeight("200px");
        responseBody.getStyle().set("font-family", "monospace");
        responseBody.getStyle().set("white-space", "pre");

        binder.forField(httpStatusCode).bind(RuleDTO::getHttpStatusCode, RuleDTO::setHttpStatusCode);
        binder.forField(responseContentType).bind(RuleDTO::getResponseContentType, RuleDTO::setResponseContentType);
        binder.forField(responseBody).bind(RuleDTO::getResponseBody, RuleDTO::setResponseBody);
        binder.forField(sleepInMillis).bind(dto -> (int)dto.getSleepInMillis(), (dto, val) -> dto.setSleepInMillis(val != null ? val : 0));
        binder.forField(orderNo).bind(RuleDTO::getOrderNo, RuleDTO::setOrderNo);

        FormLayout form = new FormLayout(orderNo, httpStatusCode, responseContentType, sleepInMillis, responseBody);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(responseBody, 2);
        add(form);
        setWidth("800px");
    }

    private void configureGroupsGrid() {
        groupsGrid.removeAllColumns();
        groupsGrid.addColumn(RuleGroupDTO::getOrderNo).setHeader("Order").setAutoWidth(true);
        groupsGrid.addColumn(g -> g.getConditions().size() + " conditions").setHeader("Conditions");
        groupsGrid.addComponentColumn(g -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openGroupDialog(g));
            Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> {
                rule.getGroups().remove(g);
                groupsGrid.setItems(rule.getGroups());
            });
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        });

        Button addGroupBtn = new Button("Add Group", VaadinIcon.PLUS.create(), e -> openGroupDialog(null));
        add(new H3("Groups"), addGroupBtn, groupsGrid);
    }

    private void openGroupDialog(RuleGroupDTO group) {
        RuleGroupDialog dialog = new RuleGroupDialog(group, savedGroup -> {
            if (group == null) {
                savedGroup.setOrderNo(rule.getGroups().size() + 1);
                rule.getGroups().add(savedGroup);
            }
            groupsGrid.setItems(rule.getGroups());
        });
        dialog.open();
    }

    private void createButtons() {
        Button saveButton = new Button("Save", e -> {
            if (binder.writeBeanIfValid(rule)) {
                onSave.accept(rule);
                close();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(saveButton, new Button("Cancel", e -> close()));
    }

    // Inner Dialog for RuleGroup
    private static class RuleGroupDialog extends Dialog {
        private final RuleGroupDTO group;
        private final Consumer<RuleGroupDTO> onSave;
        private Grid<RuleConditionDTO> conditionsGrid = new Grid<>(RuleConditionDTO.class);

        public RuleGroupDialog(RuleGroupDTO group, Consumer<RuleGroupDTO> onSave) {
            this.group = group != null ? group : new RuleGroupDTO();
            this.onSave = onSave;
            if (this.group.getConditions() == null) this.group.setConditions(new ArrayList<>());

            setHeaderTitle("Rule Group");
            configureConditionsGrid();
            
            Button saveBtn = new Button("Save", e -> {
                onSave.accept(this.group);
                close();
            });
            saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getFooter().add(saveBtn, new Button("Cancel", e -> close()));
            
            setWidth("700px");
        }

        private void configureConditionsGrid() {
            conditionsGrid.removeAllColumns();
            conditionsGrid.addColumn(RuleConditionDTO::getField).setHeader("Field");
            conditionsGrid.addColumn(c -> UIFormattingUtils.formatEnum(c.getDataType())).setHeader("Type");
            conditionsGrid.addColumn(c -> UIFormattingUtils.formatEnum(c.getComparator())).setHeader("Comparator");
            conditionsGrid.addColumn(RuleConditionDTO::getValue).setHeader("Value");
            
            conditionsGrid.addComponentColumn(c -> {
                Button del = new Button(VaadinIcon.TRASH.create(), e -> {
                    group.getConditions().remove(c);
                    conditionsGrid.setItems(group.getConditions());
                });
                del.addThemeVariants(ButtonVariant.LUMO_ERROR);
                return del;
            });

            Button addConditionBtn = new Button("Add Condition", VaadinIcon.PLUS.create(), e -> openConditionDialog());
            add(addConditionBtn, conditionsGrid);
            conditionsGrid.setItems(group.getConditions());
        }

        private void openConditionDialog() {
            Dialog d = new Dialog();
            d.setHeaderTitle("Add Condition");
            TextField field = new TextField("Field");
            ComboBox<RuleDataTypeEnum> type = new ComboBox<>("Data Type", RuleDataTypeEnum.values());
            ComboBox<RuleComparatorEnum> comp = new ComboBox<>("Comparator", RuleComparatorEnum.values());
            TextField val = new TextField("Value");
            ComboBox<RuleMatchingTypeEnum> match = new ComboBox<>("Matching Type", RuleMatchingTypeEnum.values());
            Checkbox caseSens = new Checkbox("Case Sensitive");
            
            FormLayout f = new FormLayout(field, type, comp, val, match, caseSens);
            d.add(f);
            
            Button addBtn = new Button("Add", e -> {
                RuleConditionDTO c = new RuleConditionDTO(field.getValue(), type.getValue(), comp.getValue(), val.getValue(), match.getValue(), caseSens.getValue());
                group.getConditions().add(c);
                conditionsGrid.setItems(group.getConditions());
                d.close();
            });
            addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            d.getFooter().add(addBtn, new Button("Cancel", e -> d.close()));
            d.open();
        }
    }
}
