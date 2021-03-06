package agi.core.form.generic;

import agi.core.annotations.AgiColumnValueProvider;
import agi.core.annotations.AgiEntity;
import agi.core.annotations.AgiForm;
import agi.core.annotations.Command;
import agi.core.dao.GenericDao;
import agi.core.dao.GenericDaoFactory;
import agi.core.form.IFormHandler;
import agi.core.form.util.UIOrderComparator;
import agi.core.form.util.VaadinProvidersFactory;
import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@org.springframework.stereotype.Component
@Scope("prototype")
@Slf4j
public class AbstractElementForm<T> extends VerticalLayout {

    @Value("${agi.forms.element.save}")
    protected String BTN_SAVE_TEXT;
    @Value("${agi.forms.element.reload}")
    protected String BTN_RELOAD_TEXT;
    protected T entity;
    protected Binder<T> binder;
    protected Layout defaultControlPanel;
    protected IFormHandler formHandler;
    protected String formCachedName;
    @Autowired
    GenericFieldGenerator genericFieldGenerator;
    @Autowired
    GenericDaoFactory genericDaoFactory;

    public AbstractElementForm(IFormHandler formHandler) {
        this.formHandler = formHandler;
    }

    public void init(T entity, boolean isNewInstance, String formCachedName) {
        this.formCachedName = formCachedName;
        removeAllComponents();
        this.entity = entity;
        binder = new Binder(entity.getClass());
        initControlPanel(binder);
        buildFormFields(entity);
        binder.bindInstanceFields(entity);
        if (!isNewInstance) binder.readBean(entity);
    }

    protected void buildFormFields(T entity) {
        Class clazz = entity.getClass();
        Field[] fieldsArray = clazz.getDeclaredFields();
        Method[] methodsArray = clazz.getDeclaredMethods();
        LinkedList<AnnotatedElement> elementLinkedList = createOrderedElementsList(Stream.concat(
                Stream.of(fieldsArray).filter(f -> !f.isAnnotationPresent(Autowired.class) && (f.getModifiers() & Modifier.STATIC) == 0),
                Stream.of(methodsArray).filter(m -> m.isAnnotationPresent(AgiColumnValueProvider.class))));

        for (AnnotatedElement element : elementLinkedList) {
            if (element instanceof Field) {
                Field field = (Field) element;
                Component component = genericFieldGenerator.getComponentByFieldAndBind(field, binder);
                if (component == null) continue;
                if (field.isAnnotationPresent(javax.persistence.Id.class)) {
                    ((HasValue) component).setReadOnly(true);
                }
                addComponent(component);
                component.setWidth(40f, Unit.PERCENTAGE);
            } else if (element instanceof Method) {
                Method method = (Method) element;
                if ((method.getModifiers() & Modifier.STATIC) == 0) continue;
                TextField textField = new TextField();
                genericFieldGenerator.makeUpCaptionForMethodProvidedComponent(method, textField);
                textField.setValue(String.valueOf(Objects.requireNonNull(VaadinProvidersFactory.getValueProvider(method)).apply(entity)));
                textField.setReadOnly(true);
                addComponent(textField);
            }
        }
    }

    protected LinkedList<AnnotatedElement> createOrderedElementsList(Stream<AnnotatedElement> annotatedElements) {
        return annotatedElements.sorted(new UIOrderComparator()).collect(Collectors.toCollection(LinkedList::new));
    }

    protected void initControlPanel(Binder<T> binder) {
        defaultControlPanel = new HorizontalLayout();
        MenuBar menuBar = new MenuBar();
        if (entity.getClass().isAnnotationPresent(AgiEntity.class)) {
            createSaveButton(binder, menuBar);
            createReloadButton(binder, menuBar);
        } else if (entity.getClass().isAnnotationPresent(AgiForm.class)) {
            Stream.of(entity.getClass().getDeclaredMethods())
                    .filter(m -> m.isAnnotationPresent(Command.class))
                    .forEach(method -> menuBar.addItem(method.getAnnotation(Command.class).caption(),
                            method.getAnnotation(Command.class).icon(),
                            command -> {
                                try {
                                    binder.writeBean(entity);
                                    method.invoke(entity, null);
                                    binder.readBean(entity);
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                }
                            }));
        }
        defaultControlPanel.addComponent(menuBar);
        addComponent(defaultControlPanel);
    }

    protected void createReloadButton(Binder<T> binder, MenuBar menuBar) {
        menuBar.addItem(BTN_RELOAD_TEXT,
                VaadinIcons.REFRESH,
                event -> {
                    GenericDao genericDao = genericDaoFactory.getGenericDaoForClass(entity.getClass());
                    genericDao.refresh(entity);
                    binder.readBean(entity);
                });
    }

    @SuppressWarnings("unchecked")
    protected void createSaveButton(Binder<T> binder, MenuBar menuBar) {
        menuBar.addItem(BTN_SAVE_TEXT,
                VaadinIcons.CHECK,
                (MenuBar.Command) event -> {
                    try {
                        binder.writeBean(entity);
                        GenericDao genericDao = genericDaoFactory.getGenericDaoForClass(entity.getClass());
                        entity = (T) genericDao.save(entity);
                        binder.readBean(entity); //reload autogenerated fields
                        formHandler.refreshElementCaption(entity, formCachedName);
                    } catch (Exception e) {
                        Notification.show("Error", "Error while saving element", Notification.Type.ERROR_MESSAGE);
                        log.error("Error while saving element: {}", entity.getClass().getCanonicalName(), e);
                    }
                });
    }


}
