package com.lunatech.controllers;

import org.lunatech.formidable.Form;
import org.lunatech.formidable.FormFieldWithErrors;
import org.lunatech.formidable.Validation;
import com.lunatech.models.TimeEntry;
import com.lunatech.models.TimeEntryDTO;
import com.lunatech.services.TimeEntryService;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.vavr.control.Either;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

import org.jboss.logging.Logger;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;


@Path("/times")
@Produces(MediaType.TEXT_HTML)
@ApplicationScoped
public class TimeEntryController {

    private static final Logger logger = Logger.getLogger(TimeEntryController.class);

    @Inject
    TimeEntryService timeEntryService;

    @Inject
    Template timeEntries;


    @Inject
    Template timeEntry;

    @Inject
    Template newTimeEntry;

    @Inject
    Engine engine;

    private Validation<TimeEntry> validation = new Validation<>();

    @GET
    public TemplateInstance list() {
        List<TimeEntry> entries = TimeEntry.listAll();
        return timeEntries.data("timeEntries", entries);
    }

    @GET
    @Path("/{id}")
    public TemplateInstance get(@PathParam("id") Long id) {
        TimeEntry entry = timeEntryService.findTimeEntryById(id);
        if (entry == null) {
            throw new WebApplicationException("This time entry does not exist", 404);
        }
        return timeEntry.data("timeEntry", entry);
    }

    @GET
    @Path("/new")
    public TemplateInstance prepareNew() {
        // I think since we don't have a router, that we can try to keep URI in the Controller.
        // Maybe a Qute template should not store hard-coded URI ?
        return newTimeEntry.data("zeForm", new Form("/times/new"));
    }

    @POST
    @Path("/new")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response save(@org.jboss.resteasy.annotations.Form TimeEntryDTO timeEntryDTO) {
        Either<FormFieldWithErrors, TimeEntry> validTimeEntryOrError = validation.validate(timeEntryDTO);

        return validTimeEntryOrError.fold(formErrors -> {
            logger.warn("Unable to persist a TimeEntry. Reason : " + formErrors.getErrorMessage());
            Object htmlContent = newTimeEntry.data("zeForm", new Form("/times/new", timeEntryDTO ,formErrors));
            return Response.status(400, formErrors.getErrorMessage()).entity(htmlContent).build();
        }, newTimeEntry -> {
            timeEntryService.persist(newTimeEntry);
            return Response.seeOther(URI.create("/times")).build();
        });

    }

}
