package com.sepnotican.springjpaformautocreator.generator.form;

import com.sepnotican.springjpaformautocreator.generator.annotations.AgiUI;
import com.sepnotican.springjpaformautocreator.generator.form.generic.AbstractElementForm;
import com.sepnotican.springjpaformautocreator.generator.form.generic.AbstractListForm;
import com.vaadin.ui.*;
import org.apache.log4j.Logger;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class MainFormHandler extends VerticalLayout implements IFormHandler {

    private static final String DEFAULT_LIST_FORM_PREFIX = "DEF_LIST_";
    private static final String DEFAULT_ELEMENT_FORM_PREFIX = "DEF_ELEM_";
    private TabSheet tabSheet;
    private UIHandler uiHandler;
    private Map<String, TabSheet.Tab> openedForms = new HashMap<>();
    private Layout mainLayout;

    private static final Logger logger = Logger.getLogger(MainFormHandler.class);

    public MainFormHandler(UIHandler uiHandler, Layout mainLayout) {
        this.uiHandler = uiHandler;
        this.mainLayout = mainLayout;
    }

    public void init() {
        tabSheet = new TabSheet();
        tabSheet.setCloseHandler(new TabSheet.CloseHandler() {
            @Override
            public void onTabClose(TabSheet tabsheet,
                                   Component tabContent) {
                TabSheet.Tab tab = tabsheet.getTab(tabContent);
                Notification.show("Closing " + tab.getCaption());

                if (openedForms.containsValue(tab)) {
                    openedForms.entrySet().removeIf((entry) -> entry.getValue().equals(tab));
                }
                tabsheet.removeTab(tab);
            }
        });

        this.addComponent(tabSheet);
        mainLayout.addComponent(this);
    }

    public <T> void showAbstractListForm(Class<T> aClass, JpaRepository<T, Object> jpaRepository) {
        final String formCacheName = DEFAULT_LIST_FORM_PREFIX + aClass.getCanonicalName();

        TabSheet.Tab tab;
        tab = openedForms.get(formCacheName);
        if (tab == null) {
            AbstractListForm<T, JpaRepository<T, Object>> listForm = new AbstractListForm<>(this, aClass, jpaRepository);
            tab = tabSheet.addTab(listForm);
            tab.setCaption(aClass.getAnnotation(AgiUI.class).listCaption());
            tab.setIcon(aClass.getAnnotation(AgiUI.class).icon());
            tab.setVisible(true);
            tab.setClosable(true);
            openedForms.put(formCacheName, tab);
        }

        tabSheet.setSelectedTab(tab);
        tabSheet.focus();

    }

    @Override
    public <T> void showAbstractElementForm(JpaRepository<T, Object> jpaRepository
            , T entity, boolean isNewInstance) throws NoSuchFieldException, IllegalAccessException {

        AgiUI agiUI = entity.getClass().getAnnotation(AgiUI.class);
        if (agiUI == null) throw new RuntimeException("Expected annotation is not present");
        final String formCacheName = generateElementCacheName(agiUI, entity);

        TabSheet.Tab tab;
        tab = openedForms.get(formCacheName);
        if (tab == null) {
            AbstractElementForm<T> elemForm = new AbstractElementForm<T>(jpaRepository, this);
            elemForm.init(entity, isNewInstance, formCacheName);
            tab = tabSheet.addTab(elemForm);

            String caption = generateElementCaption(entity, isNewInstance);

            tab.setCaption(agiUI.entityCaption() + ":" + caption);
            tab.setIcon(agiUI.icon());
            tab.setVisible(true);
            tab.setClosable(true);
            openedForms.put(formCacheName, tab);
        }

        tabSheet.setSelectedTab(tab);
        tabSheet.focus();
    }

    protected <T> String generateElementCaption(T entity, boolean isNewInstance) {
        String caption = "undefined";
        if (isNewInstance) {
            caption = "new";
        } else {
            for (Field field : entity.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    try {
                        caption = field.get(entity).toString();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        //todo log
                    }
                    break;
                }
            }
        }
        return caption;
    }

    @Override
    public <T> void refreshElementCaption(T entity, String cachedName) throws NoSuchFieldException, IllegalAccessException {
        AgiUI agiUI = entity.getClass().getAnnotation(AgiUI.class);
        if (agiUI == null) throw new RuntimeException("Expected annotation is absent");
        final String newCachedName = generateElementCacheName(agiUI, entity);
        TabSheet.Tab tab = openedForms.get(cachedName);
        if (tab == null) {
            logger.warn("tab not found for " + entity + "(" + entity.getClass().getCanonicalName() + ")");
            return;
        }
        tab.setCaption(generateElementCaption(entity, false));
        openedForms.remove(cachedName);
        openedForms.put(newCachedName, tab);

    }

    protected <T> String generateElementCacheName(AgiUI agiUI, T entity) throws NoSuchFieldException, IllegalAccessException {
        Field idField = entity.getClass().getDeclaredField(agiUI.idFieldName());
        idField.setAccessible(true);
        String nameAddition = String.valueOf(idField.get(entity));
        idField.setAccessible(false);
        return DEFAULT_ELEMENT_FORM_PREFIX + entity.getClass().getCanonicalName() + nameAddition;
    }

}
