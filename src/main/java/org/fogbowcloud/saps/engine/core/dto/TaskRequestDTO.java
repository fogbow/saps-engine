package org.fogbowcloud.saps.engine.core.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.saps.engine.core.task.Specification;

public class TaskRequestDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String state;
    private Specification spec;
    private List<String> commands;
    private Map<String, String> metadata;

    public TaskRequestDTO(String id, Specification spec, List<String> commands,
                          Map<String, String> metadata) {
        this.id = id;
        this.spec = spec;
        this.commands = commands;
        this.metadata = metadata;
    }

    // empty constructor required for Gson.
    public TaskRequestDTO() {}

    public List<String> getCommands() {
        return this.commands;
    }

    public Specification getSpec() {
        return this.spec;
    }

    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public String getId() {
        return id;
    }

    public String getState(){ return state; }

    @Override
    public String toString() {
        return "TaskRequestDTO [id=" + id + ", spec=" + spec + ", commands=" + commands + ", metadata="
                + metadata + "]";
    }

}