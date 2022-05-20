package it.uniba.sms2122.operassimulator.model;

/**
 * Classe che rappresenta parzialmente un'opera all'interno del museo.
 * Per questioni di compatibilità con il file Json della stanza, viene rappresentato solo l'id.
 * Gli altri dati sono superflui.
 */
public class Opera {
    private String id;

    public Opera() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
