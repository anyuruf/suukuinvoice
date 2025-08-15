package net.suuku.invoice.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import net.suuku.invoice.domain.Invoice;
import net.suuku.invoice.repository.InvoiceRepository;
import net.suuku.invoice.service.InvoiceService;
import net.suuku.invoice.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ForwardedHeaderUtils;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link net.suuku.invoice.domain.Invoice}.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceResource {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceResource.class);

    private static final String ENTITY_NAME = "suukuInvoiceInvoice";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final InvoiceService invoiceService;

    private final InvoiceRepository invoiceRepository;

    public InvoiceResource(InvoiceService invoiceService, InvoiceRepository invoiceRepository) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * {@code POST  /invoices} : Create a new invoice.
     *
     * @param invoice the invoice to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new invoice, or with status {@code 400 (Bad Request)} if the invoice has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<Invoice>> createInvoice(@Valid @RequestBody Invoice invoice) throws URISyntaxException {
        LOG.debug("REST request to save Invoice : {}", invoice);
        if (invoice.getId() != null) {
            throw new BadRequestAlertException("A new invoice cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return invoiceService
            .save(invoice)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/invoices/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /invoices/:id} : Updates an existing invoice.
     *
     * @param id the id of the invoice to save.
     * @param invoice the invoice to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated invoice,
     * or with status {@code 400 (Bad Request)} if the invoice is not valid,
     * or with status {@code 500 (Internal Server Error)} if the invoice couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Invoice>> updateInvoice(@PathVariable(required = false) final Long id, @Valid @RequestBody Invoice invoice)
        throws URISyntaxException {
        LOG.debug("REST request to update Invoice : {}, {}", id, invoice);
        if (invoice.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, invoice.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return invoiceRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return invoiceService
                    .update(invoice)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /invoices/:id} : Partial updates given fields of an existing invoice, field will ignore if it is null
     *
     * @param id the id of the invoice to save.
     * @param invoice the invoice to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated invoice,
     * or with status {@code 400 (Bad Request)} if the invoice is not valid,
     * or with status {@code 404 (Not Found)} if the invoice is not found,
     * or with status {@code 500 (Internal Server Error)} if the invoice couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<Invoice>> partialUpdateInvoice(
        @PathVariable(required = false) final Long id,
        @NotNull @RequestBody Invoice invoice
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Invoice partially : {}, {}", id, invoice);
        if (invoice.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, invoice.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return invoiceRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<Invoice> result = invoiceService.partialUpdate(invoice);

                return result
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(res ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, res.getId().toString()))
                            .body(res)
                    );
            });
    }

    /**
     * {@code GET  /invoices} : get all the invoices.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of invoices in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<Invoice>>> getAllInvoices(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request
    ) {
        LOG.debug("REST request to get a page of Invoices");
        return invoiceService
            .countAll()
            .zipWith(invoiceService.findAll(pageable).collectList())
            .map(countWithEntities ->
                ResponseEntity.ok()
                    .headers(
                        PaginationUtil.generatePaginationHttpHeaders(
                            ForwardedHeaderUtils.adaptFromForwardedHeaders(request.getURI(), request.getHeaders()),
                            new PageImpl<>(countWithEntities.getT2(), pageable, countWithEntities.getT1())
                        )
                    )
                    .body(countWithEntities.getT2())
            );
    }

    /**
     * {@code GET  /invoices/:id} : get the "id" invoice.
     *
     * @param id the id of the invoice to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the invoice, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Invoice>> getInvoice(@PathVariable Long id) {
        LOG.debug("REST request to get Invoice : {}", id);
        Mono<Invoice> invoice = invoiceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(invoice);
    }

    /**
     * {@code DELETE  /invoices/:id} : delete the "id" invoice.
     *
     * @param id the id of the invoice to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteInvoice(@PathVariable Long id) {
        LOG.debug("REST request to delete Invoice : {}", id);
        return invoiceService
            .delete(id)
            .then(
                Mono.just(
                    ResponseEntity.noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
                        .build()
                )
            );
    }
}
