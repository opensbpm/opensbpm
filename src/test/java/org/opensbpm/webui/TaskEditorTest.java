package org.opensbpm.webui;

import org.opensbpm.engine.api.instance.TaskInfo;
import org.opensbpm.engine.core.engine.UserService;
import org.opensbpm.engine.core.engine.entities.User;
import org.opensbpm.webui.backend.SbpmEngine;
import org.opensbpm.webui.ui.MainLayout;
import org.springframework.security.test.context.support.WithMockUser;
import com.github.mvysny.kaributesting.v10.*;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.SpringServlet;

import kotlin.jvm.functions.Function0;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensbpm.engine.api.instance.ObjectSchema;
import org.opensbpm.engine.api.instance.SimpleAttributeSchema;
import org.opensbpm.engine.api.instance.Task;
import org.opensbpm.engine.api.instance.TaskResponse;
import org.opensbpm.engine.api.model.FieldType;
import org.opensbpm.webui.ui.views.TaskEditor;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;

import org.springframework.boot.test.mock.mockito.MockBean;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@DirtiesContext
public class TaskEditorTest {

    private final static String USERNAME = "user";

    private static Routes routes;

    @BeforeAll
    public static void discoverRoutes() {
        routes = new Routes().autoDiscoverViews(MainLayout.class.getPackageName());
    }

    @Autowired
    private ApplicationContext ctx;

    @MockBean
    private UserService userService;

    @MockBean
    private SbpmEngine sbpmEngine;

    @BeforeEach
    public void setup() {
        User user = spy(new User(USERNAME));
        when(user.getId()).thenReturn(1l);

        when(userService.findByName(anyString())).thenReturn(Optional.of(user));
        when(userService.findById(anyLong())).thenReturn(Optional.of(user));

        Function0<UI> uiFactory = UI::new;
        SpringServlet servlet = new MockSpringServlet(routes, ctx, uiFactory);
        MockVaadin.setup(uiFactory, servlet);
    }

    @AfterEach
    public void tearDown() {
        MockVaadin.tearDown();
    }

    @WithMockUser(USERNAME)
    @Test
    public void editTask() throws Exception {
        //given
        long id = 0l;
        TaskInfo taskInfo = new TaskInfo(id++, Long.MIN_VALUE, "Test Process", "Test State", LocalDateTime.now());

        when(sbpmEngine.getTasks(anyString())).then(iom -> Stream.of(taskInfo));

        ObjectSchema objectSchema = ObjectSchema.of(id++, "Test", asList(
                SimpleAttributeSchema.of(id++, "string", FieldType.STRING)
        ));
        TaskResponse taskResponse = TaskResponse.of(Long.MIN_VALUE, Collections.emptyList(),
                LocalDateTime.MIN, Arrays.asList(objectSchema), Collections.emptyList());
        when(sbpmEngine.getTask(anyLong())).thenReturn(new Task(taskInfo, taskResponse));

        UI.getCurrent().navigate(TaskEditor.class, TaskEditor.createTaskParameter(taskInfo));
        //expectRows(_get(Grid.class), 0);

        //when
        UI.getCurrent().getPage().reload();

        //then
        //expectRow(_get(Grid.class), 0, "Test Process", "Test State");
    }
}
