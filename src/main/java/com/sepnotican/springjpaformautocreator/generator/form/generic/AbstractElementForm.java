package com.sepnotican.springjpaformautocreator.generator.form.generic;

import com.sepnotican.springjpaformautocreator.generator.annotations.BigString;
import com.sepnotican.springjpaformautocreator.generator.annotations.Synonym;
import com.sepnotican.springjpaformautocreator.generator.annotations.UIDrawOrder;
import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.data.ValidationException;
import com.vaadin.data.converter.StringToDoubleConverter;
import com.vaadin.data.converter.StringToFloatConverter;
import com.vaadin.data.converter.StringToLongConverter;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class AbstractElementForm<T> extends GridLayout {

    public static final String BTN_SAVE_TEXT = "Save";
    public static final String BTN_RELOAD_TEXT = "Reload";
    public static final String EMPTY_ENUM_TEXT = "<empty>";

    protected T entity;
    protected Binder<T> binder;
    protected Layout defaultControlPanel;

    private JpaRepository<T, Object> repository;

    public AbstractElementForm(JpaRepository<T, Object> repository) {
        this.repository = repository;
    }

    public void init(T entity) {
        removeAllComponents();

        this.entity = entity;
        binder = new Binder(entity.getClass());

        initDefaultControlPanel(binder);

        Class clazz = entity.getClass();
        Field[] fieldsArray = clazz.getDeclaredFields();
        ArrayList<Field> fieldArrayList = createOrderedElementsList(fieldsArray);

        for (Field field : fieldArrayList) {

            Component component = getComponentByFieldAndBind(field, binder);

            if (component == null) continue;

            if (field.isAnnotationPresent(javax.persistence.Id.class))
                ((HasValue) component).setReadOnly(true);

            makeUpCaptionForField(field, component);

            addComponent(component);

        }
        binder.bindInstanceFields(entity);
        binder.readBean(entity);

    }

    protected ArrayList<Field> createOrderedElementsList(Field[] fieldsArray) {
        return new ArrayList<>(Arrays.stream(fieldsArray)
                .sorted(new UIOrderComparator())
                .collect(Collectors.toList()));
    }

    protected class UIOrderComparator implements Comparator<Field> {

        @Override
        public int compare(Field o1, Field o2) {
            if (!o1.isAnnotationPresent(UIDrawOrder.class)
                    && o2.isAnnotationPresent(UIDrawOrder.class))
                return 1;
            else if (o1.isAnnotationPresent(UIDrawOrder.class)
                    && !o2.isAnnotationPresent(UIDrawOrder.class))
                return -1;
            else if (!o1.isAnnotationPresent(UIDrawOrder.class)
                    && !o2.isAnnotationPresent(UIDrawOrder.class))
                return 0;
            else if (o1.getAnnotation(UIDrawOrder.class).drawOrder() >
                    o2.getAnnotation(UIDrawOrder.class).drawOrder())
                return 1;
            else if ((o1.getAnnotation(UIDrawOrder.class).drawOrder() <
                    o2.getAnnotation(UIDrawOrder.class).drawOrder()))
                return -1;

            return 0;
        }
    }


    protected void initDefaultControlPanel(Binder binder) {
        defaultControlPanel = new HorizontalLayout();

        MenuBar menuBar = new MenuBar();
        MenuBar.MenuItem menuItemSave = menuBar.addItem(BTN_SAVE_TEXT,
                VaadinIcons.CHECK,
                event -> {
            try {
                binder.writeBean(entity);
                repository.save(entity);
            } catch (ValidationException e) {
                e.printStackTrace();
            }
        });

        MenuBar.MenuItem menuItemReload = menuBar.addItem(BTN_RELOAD_TEXT,
                VaadinIcons.REFRESH,
                event -> binder.readBean(entity));


        defaultControlPanel.addComponent(menuBar);
        addComponent(defaultControlPanel);
    }

    protected void makeUpCaptionForField(Field field, Component component) {
        if (field.isAnnotationPresent(Synonym.class)) {
            component.setCaption(field.getAnnotation(Synonym.class).value());
        } else component.setCaption(field.getName());
    }

    protected Component getComponentByFieldAndBind(Field field, Binder binder) {
        if (field.getType().equals(Long.class)
                || field.getType().equals(long.class)) {
            return generateLongField(field, binder);

        } else if (field.getType().equals(Double.class)
                || field.getType().equals(double.class)) {
            return generateDoubleFieild(field, binder);

        } else if (field.getType().equals(Float.class)
                || field.getType().equals(float.class)) {
            return generateFloatFieild(field, binder);

        } else if (field.getType().equals(String.class)) {
            return generateStringField(field, binder);

        } else if (field.getType().isEnum()) {
            return generateEnumField(field, binder);

        } else return null;
    }

    protected Component generateFloatFieild(Field field, Binder binder) {
        TextField textField = new TextField(field.getName());
        binder.forField(textField)
                .withConverter(new StringToFloatConverter("Must be a float value"))
                .bind(field.getName());
        return textField;
    }

    protected Component generateDoubleFieild(Field field, Binder binder) {
        TextField textField = new TextField(field.getName());
        binder.forField(textField)
                .withConverter(new StringToDoubleConverter("Must be a double value"))
                .bind(field.getName());
        return textField;
    }

    protected Component generateEnumField(Field field, Binder binder) {
        Class clazzEnum = field.getType();
        ComboBox comboBox = new ComboBox<>();
        comboBox.setEmptySelectionCaption(EMPTY_ENUM_TEXT);
        comboBox.setItems(field.getType().getEnumConstants());
        binder.bind(comboBox, field.getName());
        return comboBox;
    }

    protected Component generateStringField(Field field, Binder binder) {

        Component textField = null;
        if (field.isAnnotationPresent(BigString.class)) {
            textField = new TextArea();
        } else textField = new TextField();
        binder.bind((HasValue) textField, field.getName());
        return textField;
    }

    protected Component generateLongField(Field field, Binder binder) {
        TextField textField = new TextField(field.getName());
        binder.forField(textField)
                .withConverter(new StringToLongConverter("Must be a Long value"))
                .bind(field.getName());
        return textField;
    }


}
