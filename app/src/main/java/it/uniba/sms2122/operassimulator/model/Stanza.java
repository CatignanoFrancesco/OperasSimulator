package it.uniba.sms2122.operassimulator.model;

import java.util.Map;

/**
 * Classe che rappresenta una semplice stanza di un museo. Qui verranno prese le informazioni circa le opere presenti.
 */
public class Stanza {
    private String id;
    private String nome;
    private String descrizione;
    private Map<String, Opera> opere;

    public Stanza() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Map<String, Opera> getOpere() {
        return opere;
    }

    public void setOpere(Map<String, Opera> opere) {
        this.opere = opere;
    }
}
