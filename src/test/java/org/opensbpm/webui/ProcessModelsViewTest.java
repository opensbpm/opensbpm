package org.opensbpm.webui;

import org.opensbpm.engine.core.engine.UserService;
import org.opensbpm.engine.core.engine.entities.User;
import org.opensbpm.webui.ui.MainLayout;
import org.springframework.security.test.context.support.WithMockUser;
import com.github.mvysny.kaributesting.v10.*;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.spring.SpringServlet;

import kotlin.jvm.functions.Function0;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import static com.github.mvysny.kaributesting.v10.GridKt.*;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensbpm.engine.api.ModelService;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.api.model.ProcessModelState;
import org.opensbpm.webui.ui.views.ProcessModelsView;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@DirtiesContext
public class ProcessModelsViewTest {

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
    private ModelService modelService;

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
    public void listModels() throws Exception {
        UI.getCurrent().navigate(ProcessModelsView.class);
        expectRows(_get(Grid.class), 0);

        //given
        ProcessModelInfo processModelInfo = new ProcessModelInfo(Long.MIN_VALUE, "Test ProcessModel",
                "1.0",
                "Description",
                ProcessModelState.ACTIVE,
                LocalDateTime.now(),
                Collections.emptyList());
        when(modelService.findAllByStates(any(Set.class))).thenReturn(Arrays.asList(processModelInfo));

        //when
        UI.getCurrent().getPage().reload();

        //then
        expectRows(_get(Grid.class), 1);
        expectRow(_get(Grid.class), 0, "Test ProcessModel", "1.0", "Description", "ACTIVE");
    }
}
