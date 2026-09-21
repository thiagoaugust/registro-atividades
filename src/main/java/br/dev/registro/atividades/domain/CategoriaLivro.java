package br.dev.registro.atividades.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Genero do livro (tecnico, literatura, historia...). Tabela, e nao enum, porque a lista e sua: uma
 * categoria nova e um cadastro, nao um deploy.
 *
 * <p>Nao confundir com {@link Categoria}, que classifica a atividade (TREINO, ESTUDO, LEITURA...).
 */
@Entity
@Table(name = "categoria_livro")
public class CategoriaLivro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String nome;

    @Column(nullable = false)
    public boolean ativa = true;

    @Column(nullable = false)
    public int ordem;
}
