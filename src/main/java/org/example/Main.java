package org.example;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("biblioteca");
        GestorBiblioteca gestor = new GestorBiblioteca(emf);

        Scanner sc = new Scanner(System.in);
        int opcion = 0;

        do {
            System.out.println("\n--- GESTION BIBLIOTECA ---");
            System.out.println("1. Registrar Libro");
            System.out.println("2. Registrar Ejemplar");
            System.out.println("3. Registrar Usuario");
            System.out.println("4. Stock Disponible");
            System.out.println("5. PRESTAR");
            System.out.println("6. DEVOLVER");
            System.out.println("7. Ver Préstamos");
            System.out.println("0. Salir");
            System.out.print("Elige: ");
            opcion = sc.nextInt();
            sc.nextLine();

            switch (opcion) {
                case 1:
                    System.out.print("ISBN: "); String isbn = sc.nextLine();
                    System.out.print("Titulo: "); String tit = sc.nextLine();
                    System.out.print("Autor: "); String aut = sc.nextLine();
                    gestor.registrarLibro(isbn, tit, aut);
                    break;
                case 2:
                    System.out.print("ISBN Libro: "); String isbne = sc.nextLine();
                    gestor.registrarEjemplar(isbne, "Disponible");
                    break;
                case 3:
                    System.out.print("DNI: "); String dni = sc.nextLine();
                    System.out.print("Nombre: "); String nom = sc.nextLine();
                    System.out.print("Email: "); String mail = sc.nextLine();
                    System.out.print("Pass: "); String pass = sc.nextLine();
                    System.out.print("Tipo: "); String tipo = sc.nextLine();
                    gestor.registrarUsuario(dni, nom, mail, pass, tipo);
                    break;
                case 4: gestor.verStockDisponible(); break;
                case 5:
                    System.out.print("DNI Usuario: "); String du = sc.nextLine();
                    System.out.print("ID Ejemplar: "); int ide = sc.nextInt();
                    gestor.realizarPrestamo(du, ide);
                    break;
                case 6:
                    System.out.print("ID Prestamo: "); int idp = sc.nextInt();
                    gestor.devolverEjemplar(idp);
                    break;
                case 7:
                    System.out.print("Tu DNI: "); String midni = sc.nextLine();
                    gestor.listarPrestamos(midni);
                    break;
            }

        } while (opcion != 0);
        emf.close();
    }
}