package at.softwaremacherei.jsbpm.webui.ui.views.dashboard;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import at.softwaremacherei.jsbpm.webui.backend.service.CompanyService;
import at.softwaremacherei.jsbpm.webui.backend.service.ContactService;
import at.softwaremacherei.jsbpm.webui.ui.MainLayout;

@PageTitle("Dashboard | Vaadin CRM")
@Route(value = "dashboard", layout = MainLayout.class)
public class DashboardView extends VerticalLayout {

    private final ContactService contactService;
    private final CompanyService companyService;

    public DashboardView(ContactService contactService,
                         CompanyService companyService) {
        this.contactService = contactService;
        this.companyService = companyService;

        addClassName("dashboard-view");
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        add(
            getContactStats(),
            getCompaniesChart()
        );
    }

    private Span getContactStats() {
        Span stats = new Span(contactService.count() + " contacts");
        stats.addClassName("contact-stats");

        return stats;
    }

    private Component getCompaniesChart() {
//        Chart chart = new Chart(ChartType.PIE);
//
//        DataSeries dataSeries = new DataSeries();
//        Map<String, Integer> stats = companyService.getStats();
//        stats.forEach((name, number) ->
//            dataSeries.add(new DataSeriesItem(name, number)));
//
//        chart.getConfiguration().setSeries(dataSeries);
//        return chart;
        return new Label("TODO ad some content");
    }
}
