package ec.edu.uteq.appweb.biblioteca.web.controller;

import ec.edu.uteq.appweb.biblioteca.domain.Libro;
import ec.edu.uteq.appweb.biblioteca.service.LibroService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.LibroResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.PageMeta;
import ec.edu.uteq.appweb.biblioteca.web.mapper.LibroMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * TODO-U4-1 (Objetivo especifico 2 de la Guia): API REST DEL CATALOGO
 * ============================================================================
 *
 * Replica el patron de AutorController, que ya esta implementado y comentado.
 * LibroService y LibroMapper estan completos: solo se expone, no se reimplementa.
 *
 * Endpoints exigidos:
 *   GET    /api/v1/libros                 paginado, con meta; parametros opcionales
 *                                         titulo, categoriaId y anioDesde -> LibroService.buscar
 *   GET    /api/v1/libros/{id}            200 o 404 con ProblemDetail
 *   POST   /api/v1/libros                 201 + Location, rol ADMIN
 *   PUT    /api/v1/libros/{id}            200, rol ADMIN
 *   DELETE /api/v1/libros/{id}            204, rol ADMIN, borrado logico
 *   GET    /api/v1/libros/{id}/enriquecido combina el libro local con Open Library
 *                                         (depende del TODO-U4-4)
 *
 * Recuerde: exito en ApiResponse, error en ProblemDetail, nunca los dos mezclados.
 */
@RestController
@RequestMapping("/api/v1/libros")
public class LibroController {

    private final LibroService servicio;
    private final LibroMapper mapper;

    public LibroController(LibroService servicio, LibroMapper mapper) {
        this.servicio = servicio;
        this.mapper = mapper;
    }

    @GetMapping
    public ApiResponse<List<LibroResponse>> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Integer anioDesde,
            @PageableDefault(size = 20) Pageable paginacion) {
        Page<Libro> pagina = servicio.buscar(titulo, categoriaId, anioDesde, paginacion);
        List<LibroResponse> datos = pagina.getContent().stream().map(mapper::aRespuesta).toList();
        return ApiResponse.ok(datos, "Libros listados", PageMeta.de(pagina));
    }

    @GetMapping("/{id}")
    public ApiResponse<LibroResponse> buscar(@PathVariable Long id) {
        return ApiResponse.ok(mapper.aRespuesta(servicio.buscarPorId(id)), "Libro encontrado");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LibroResponse>> crear(@Valid @RequestBody LibroRequest solicitud) {
        Libro creado = servicio.crear(solicitud);
        LibroResponse cuerpo = mapper.aRespuesta(creado);
        return ResponseEntity
                .created(URI.create("/api/v1/libros/" + creado.getId()))
                .body(ApiResponse.ok(cuerpo, "Libro creado"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LibroResponse> actualizar(@PathVariable Long id, @Valid @RequestBody LibroRequest solicitud) {
        Libro actualizado = servicio.actualizar(id, solicitud);
        return ApiResponse.ok(mapper.aRespuesta(actualizado), "Libro actualizado");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
