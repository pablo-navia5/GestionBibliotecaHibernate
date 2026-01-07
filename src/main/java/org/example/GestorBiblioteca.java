package org.example;

import Entidades.Ejemplar;
import Entidades.Libro;
import Entidades.Prestamo;
import Entidades.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;

public class GestorBiblioteca {
    private EntityManagerFactory emf;

    public GestorBiblioteca(EntityManagerFactory emf) {
        this.emf = emf;
    }

    private Usuario buscarUsuarioPorDni(EntityManager em, String dni) {
        try {
            TypedQuery<Usuario> query = em.createQuery("SELECT u FROM Usuario u WHERE u.dni = :dni", Usuario.class);
            query.setParameter("dni", dni);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void registrarLibro(String isbn, String titulo, String autor) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Libro libro = new Libro();
            libro.setIsbn(isbn);
            libro.setTitulo(titulo);
            libro.setAutor(autor);
            em.persist(libro);
            em.getTransaction().commit();
            System.out.println("Libro registrado.");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void registrarEjemplar(String isbnLibro, String estado) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Libro libro = em.find(Libro.class, isbnLibro);

            if (libro != null) {
                Ejemplar ejemplar = new Ejemplar();
                ejemplar.setIsbn(libro);
                ejemplar.setEstado(estado);
                em.persist(ejemplar);
                em.getTransaction().commit();
                System.out.println("Ejemplar registrado.");
            } else {
                System.out.println("Libro no encontrado.");
            }
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void verStockDisponible() {
        EntityManager em = emf.createEntityManager();
        List<Ejemplar> ejemplares = em.createQuery("SELECT e FROM Ejemplar e", Ejemplar.class).getResultList();

        int cantidadEjemplares = 0;

        for (Ejemplar e : ejemplares) {
            if ("Disponible".equals(e.getEstado())) {
                cantidadEjemplares++;
            }
        }

        System.out.println("Stock Disponible: " + cantidadEjemplares);
        em.close();
    }

    public void registrarUsuario(String dni, String nombre, String email, String pass, String tipo) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = new Usuario();
            usuario.setDni(dni);
            usuario.setNombre(nombre);
            usuario.setEmail(email);
            usuario.setPassword(pass);
            usuario.setTipo(tipo);
            em.persist(usuario);
            em.getTransaction().commit();
            System.out.println("Usuario creado.");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            System.out.println("Error: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public void realizarPrestamo(String dniUsuario, int idEjemplar) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Usuario usuario = buscarUsuarioPorDni(em, dniUsuario);
            Ejemplar ejemplar = em.find(Ejemplar.class, idEjemplar);

            if (usuario == null || ejemplar == null) {
                System.out.println("Usuario o Ejemplar no existen.");
                return;
            }

            //Compruebo si esta penalizado o no
            if (usuario.getPenalizacionHasta() != null && usuario.getPenalizacionHasta().isAfter(LocalDate.now())) {
                System.out.println("Usuario PENALIZADO hasta " + usuario.getPenalizacionHasta());
                return;
            }

            //Miro si el ejemplar esta disponible
            if (!"Disponible".equalsIgnoreCase(ejemplar.getEstado())) {
                System.out.println("Ejemplar no disponible.");
                return;
            }


            TypedQuery<Long> countQ = em.createQuery("SELECT count(p) FROM Prestamo p WHERE p.usuario.id = :uid AND p.fechaDevolucion IS NULL", Long.class);
            countQ.setParameter("uid", usuario.getId());
            if (countQ.getSingleResult() >= 3) {
                System.out.println("Ya tienes 3 préstamos activos.");
                return;
            }

            //CReamos el prestamo y ponemos el ejemplar ese como prestado
            Prestamo prestamo = new Prestamo();
            prestamo.setUsuario(usuario);
            prestamo.setEjemplar(ejemplar);
            prestamo.setFechaInicio(LocalDate.now());
            ejemplar.setEstado("Prestado");

            em.persist(prestamo);
            em.merge(ejemplar);

            em.getTransaction().commit();
            System.out.println("Préstamo realizado.");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void devolverEjemplar(int idPrestamo) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Prestamo prestamo = em.find(Prestamo.class, idPrestamo);

            if (prestamo == null || prestamo.getFechaDevolucion() != null) {
                System.out.println("Préstamo inválido o ya devuelto.");
                return;
            }

            prestamo.setFechaDevolucion(LocalDate.now());


            Ejemplar ejemplar = prestamo.getEjemplar();
            ejemplar.setEstado("Disponible");
            em.merge(ejemplar);

            //Miramos si ha sido devuelto a tiempo o si se a pasado y hay que penalizar al usuario
            LocalDate fechaLimite = prestamo.getFechaInicio().plusDays(15);
            if (LocalDate.now().isAfter(fechaLimite)) {
                Usuario usuario = prestamo.getUsuario();

                LocalDate inicio = LocalDate.now();
                if(usuario.getPenalizacionHasta() != null && usuario.getPenalizacionHasta().isAfter(inicio)) {
                    inicio = usuario.getPenalizacionHasta();
                }
                usuario.setPenalizacionHasta(inicio.plusDays(15));
                em.merge(usuario);
                System.out.println("DEVOLUCIÓN TARDÍA. Penalización añadida.");
            }

            em.merge(prestamo);
            em.getTransaction().commit();
            System.out.println("Libro devuelto.");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void listarPrestamos(String dni) {
        EntityManager em = emf.createEntityManager();
        try {
            Usuario usuario = buscarUsuarioPorDni(em, dni);
            if (usuario == null) {
                System.out.println("Usuario no encontrado");
                return;
            }

            List<Prestamo> lista;
            if ("administrador".equals(usuario.getTipo())) {
                lista = em.createQuery("SELECT p FROM Prestamo p", Prestamo.class).getResultList();
            } else {
                TypedQuery<Prestamo> q = em.createQuery("SELECT p FROM Prestamo p WHERE p.usuario.id = :uid", Prestamo.class);
                q.setParameter("uid", usuario.getId());
                lista = q.getResultList();
            }

            System.out.println("--- LISTA DE PRÉSTAMOS ---");
            for (Prestamo p : lista) {
                System.out.println("ID Prestamo: " + p.getId()
                        + " | Fecha Inicio: " + p.getFechaInicio()
                        + " | Estado: " + (p.getFechaDevolucion()==null ? "ACTIVO" : "DEVUELTO"));
            }
        } finally {
            em.close();
        }
    }
}
