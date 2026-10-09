package com.fidelg.documentgenerator.domain.demand;

import java.util.Objects;
import java.util.UUID;

/**
 * Elemento de una colección ordenada. El orden es la posición en la lista de la
 * sección y la numeración se deriva de él: nunca se almacena en el elemento.
 */
public class CollectionItem {

    private String id;
    private String title;
    private String content;

    public CollectionItem() {
        this(null, "");
    }

    public CollectionItem(String title, String content) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.content = content == null ? "" : content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CollectionItem item)) {
            return false;
        }
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
