package agi.core.form;

import agi.core.annotations.AgiEntity;
import agi.core.annotations.AgiForm;
import agi.core.form.generic.AbstractElementForm;
import agi.core.form.generic.AbstractListForm;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@org.springframework.stereotype.Component
@Slf4j
@Scope("prototype")
public class MainFormHandler extends VerticalLayout implements IFormHandler {

    private static final String DEFAULT_LIST_FORM_PREFIX = "DEF_LIST_";
    private static final String DEFAULT_ELEMENT_FORM_PREFIX = "DEF_ELEM_";
    @Autowired
    ApplicationContext context;
    private TabSheet tabSheet;
    private Map<String, TabSheet.Tab> openedForms = new HashMap<>();
    private AbstractOrderedLayout mainLayout;

    public MainFormHandler() {
        mainLayout = new VerticalLayout();
        mainLayout.setHeightUndefined();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(false);
        mainLayout.setMargin(false);
        this.setHeightUndefined();
        this.setSizeFull();
    }

    @Override
    public AbstractOrderedLayout getMainLayout() {
        return this;
    }

    @Override
    public void handleFilterException(String fieldName, Exception ex) {
        if (ex instanceof NumberFormatException) {
            Notification.show(String.format("Value in field \"%s\" must be a number", fieldName));
        } else {
            Notification.show(String.format("Value in field \"%s\" is wrong", fieldName));
        }
    }

    @PostConstruct
    public void init() {
        tabSheet = new TabSheet();
        tabSheet.setCloseHandler((TabSheet.CloseHandler) (tabsheet, tabContent) -> {
            TabSheet.Tab tab = tabsheet.getTab(tabContent);
            Notification.show("Closing " + tab.getCaption());

            if (openedForms.containsValue(tab)) {
                openedForms.entrySet().removeIf((entry) -> entry.getValue().equals(tab));
            }
            tabsheet.removeTab(tab);
        });
        tabSheet.setHeight("100%");
        mainLayout.addComponent(tabSheet);
        mainLayout.setExpandRatio(tabSheet, 1.0f);
        this.addComponent(mainLayout);
        this.setExpandRatio(mainLayout, 1.0f);
    }

    public <T> void showAbstractListForm(Class<T> aClass) {
        final String formCacheName = DEFAULT_LIST_FORM_PREFIX + aClass.getCanonicalName();

        TabSheet.Tab tab;
        tab = openedForms.get(formCacheName);
        if (tab == null) {
            AbstractListForm<T> listForm = context.getBean(AbstractListForm.class, this, aClass);
            tab = tabSheet.addTab(listForm);
            tab.setCaption(aClass.getAnnotation(AgiEntity.class).menuCaption());
            tab.setIcon(aClass.getAnnotation(AgiEntity.class).icon());
            tab.setVisible(true);
            tab.setClosable(true);
            openedForms.put(formCacheName, tab);
        }

        tabSheet.setSelectedTab(tab);
        tabSheet.focus();

    }

    @Override
    public <T> void showAbstractElementForm(T entity, boolean isNewInstance) {

        VaadinIcons icon;
        if (entity.getClass().isAnnotationPresent(AgiEntity.class)) {
            icon = entity.getClass().getAnnotation(AgiEntity.class).icon();
        } else if (entity.getClass().isAnnotationPresent(AgiForm.class)) {
            icon = entity.getClass().getAnnotation(AgiForm.class).icon();
        } else throw new RuntimeException("Expected AGI annotation is not present");
        final String formCacheName = generateElementCacheName(entity);

        TabSheet.Tab tab;
        tab = openedForms.get(formCacheName);
        if (tab == null) {
            AbstractElementForm<T> elemForm = context.getBean(AbstractElementForm.class, this);
            elemForm.init(entity, isNewInstance, formCacheName);
            tab = tabSheet.addTab(elemForm);

            String caption = generateElementCaption(entity, isNewInstance);

            tab.setCaption(caption);
            tab.setIcon(icon);
            tab.setVisible(true);
            tab.setClosable(true);
            openedForms.put(formCacheName, tab);
        }

        tabSheet.setSelectedTab(tab);
        tabSheet.focus();
    }

    protected <T> String generateElementCaption(T entity, boolean isNewInstance) {
        if (!entity.getClass().isAnnotationPresent(AgiEntity.class)
                && !entity.getClass().isAnnotationPresent(AgiForm.class))
            throw new RuntimeException("Unacceptable class, annotation AgiEntity or AgiForm is necessary");

        String caption;
        if (entity.getClass().isAnnotationPresent(AgiEntity.class)) {
            caption = entity.getClass().getAnnotation(AgiEntity.class).singleCaption() + ':';
        } else {
            caption = entity.getClass().getAnnotation(AgiForm.class).caption() + ':';
        }
        if (isNewInstance) {
            caption += "new";
        } else {
            for (Field field : entity.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    try {
                        caption += field.get(entity).toString();
                    } catch (IllegalAccessException e) {
                        log.error(e.getMessage());
                    }
                    break;
                }
            }
        }
        return caption;
    }

    @Override
    public <T> void refreshElementCaption(T entity, String cachedName) {
        final String newCachedName = generateElementCacheName(entity);
        TabSheet.Tab tab = openedForms.get(cachedName);
        if (tab == null) {
            log.warn("tab not found for cached name: " + cachedName);
            return;
        }
        tab.setCaption(generateElementCaption(entity, false));
        openedForms.remove(cachedName);
        openedForms.put(newCachedName, tab);

    }

    protected <T> String generateElementCacheName(T entity) {
        return DEFAULT_ELEMENT_FORM_PREFIX + entity.getClass().getCanonicalName() + entity.hashCode();
    }

}
