package ec.edu.uteq.appweb.biblioteca.repository;

import ec.edu.uteq.appweb.biblioteca.domain.Libro;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Capa de acceso a datos del catalogo (Unidad III). COMPLETA.
 *
 * La busqueda con filtros opcionales se resuelve con Specification
 * (JpaSpecificationExecutor) y no con un @Query de parametros anulables.
 * El motivo es concreto: en PostgreSQL una expresion del tipo
 * ":parametro is null" sobre un parametro sin tipo hace fallar la inferencia
 * de tipos del driver con el error "could not determine data type of parameter".
 * Specification construye el predicado en Java y solo agrega al WHERE los
 * filtros que realmente vienen informados.
 *
 * Nota de integracion (Unidad IV): las asociaciones autor, editorial y categoria
 * son LAZY. Al estar spring.jpa.open-in-view=false, el mapeo a DTO en el
 * controlador (fuera de la transaccion del servicio) dispararia
 * LazyInitializationException. Por eso las lecturas usan @EntityGraph para
 * cargar esas tres asociaciones junto con el libro.
 */
public interface LibroRepository extends JpaRepository<Libro, Long>, JpaSpecificationExecutor<Libro> {

    Optional<Libro> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @EntityGraph(attributePaths = {"autor", "editorial", "categoria"})
    Page<Libro> findByActivoTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"autor", "editorial", "categoria"})
    Page<Libro> findAll(Specification<Libro> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"autor", "editorial", "categoria"})
    Optional<Libro> findById(Long id);
}
