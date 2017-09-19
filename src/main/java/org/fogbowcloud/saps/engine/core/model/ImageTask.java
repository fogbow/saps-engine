package org.fogbowcloud.saps.engine.core.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class ImageTask implements Serializable {

    private String taskId;
    private String name;
    private String downloadLink;
    private ImageTaskState state;
    private String federationMember;
    private int priority;
    private String stationId;
    private String containerRepository;
    private String containerTag;
    private String crawlerVersion;
    private String fetcherVersion;
    private String blowoutVersion;
    private String fmaskVersion;
    private Timestamp creationTime;
    private Timestamp updateTime;
    private String status;
    private String error;
    private String collectionTierName;

    private Map<String, Integer> tasksStatesCount = new HashMap<String, Integer>();

    public static final String AVAILABLE = "available";
    public static final String PURGED = "purged";

    public static final String NON_EXISTENT = "NE";

    public ImageTask(String taskId, String name, String downloadLink, ImageTaskState state,
                     String federationMember, int priority, String stationId, String containerRepository,
                     String containerTag, String crawlerVersion, String fetcherVersion,
                     String blowoutVersion, String fmaskVersion, Timestamp creationTime,
                     Timestamp updateTime, String status, String error, String collectionTierName) {
        this.taskId = taskId;
        this.name = name;
        this.downloadLink = downloadLink;
        this.state = state;
        this.federationMember = federationMember;
        this.priority = priority;
        this.stationId = stationId;
        this.containerRepository = containerRepository;
        this.containerTag = containerTag;
        this.crawlerVersion = crawlerVersion;
        this.fetcherVersion = fetcherVersion;
        this.blowoutVersion = blowoutVersion;
        this.fmaskVersion = fmaskVersion;
        this.creationTime = creationTime;
        this.updateTime = updateTime;
        this.status = status;
        this.error = error;
        this.collectionTierName = collectionTierName;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public ImageTaskState getState() {
        return state;
    }

    public String getFederationMember() {
        return federationMember;
    }

    public int getPriority() {
        return priority;
    }

    public String getStationId() {
        return stationId;
    }

    public String getContainerRepository() {
        return containerRepository;
    }

    public String getContainerTag() {
        return containerTag;
    }

    public String getCrawlerVersion() {
        return crawlerVersion;
    }

    public String getFetcherVersion() {
        return fetcherVersion;
    }

    public String getBlowoutVersion() {
        return blowoutVersion;
    }

    public String getFmaskVersion() {
        return fmaskVersion;
    }

    public Timestamp getCreationTime() {
        return creationTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public String getImageStatus() {
        return status;
    }

    public String getImageError() {
        return error;
    }

    public String getCollectionTierName() {
        return collectionTierName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    public void setState(ImageTaskState state) {
        this.state = state;
    }

    public void setFederationMember(String federationMember) {
        this.federationMember = federationMember;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public void setContainerRepository(String containerRepository) {
        this.containerRepository = containerRepository;
    }

    public void setContainerTag(String containerTag) {
        this.containerTag = containerTag;
    }

    public void setCrawlerVersion(String crawlerVersion) {
        this.crawlerVersion = crawlerVersion;
    }

    public void setFetcherVersion(String fetcherVersion) {
        this.fetcherVersion = fetcherVersion;
    }

    public void setBlowoutVersion(String blowoutVersion) {
        this.blowoutVersion = blowoutVersion;
    }

    public void setFmaskVersion(String fmaskVersion) {
        this.fmaskVersion = fmaskVersion;
    }

    public void setCreationTime(Timestamp creationTime) {
        this.creationTime = creationTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

    public void setImageStatus(String status) {
        this.status = status;
    }

    public void setImageError(String error) {
        this.error = error;
    }

    public void setCollectionTierName(String collectionTierName) {
        this.collectionTierName = collectionTierName;
    }

    public String toString() {
        return "[" + taskId + ", " + name + ", " + downloadLink + ", " + state.getValue() + ", "
                + federationMember + ", " + priority + ", " + stationId + ", "
                + containerRepository + ", " + containerTag + ", " + crawlerVersion + ", "
                + fetcherVersion + ", " + blowoutVersion + ", " + fmaskVersion + ", "
                + creationTime + ", " + updateTime + ", " + status + ", " + error + ", "
                + collectionTierName + "]";
    }

    public String formatedToString() {

        return "[ TaskId = " + taskId + " ]\n" + "[ ImageName = " + name + " ]\n"
                + "[ DownloadLink = " + downloadLink + " ]\n" + "[ ImageState = "
                + state.getValue() + " ]\n" + "[ FederationMember = " + federationMember + " ]\n"
                + "[ Priority = " + priority + " ]\n" + "[ StationId = " + stationId + " ]\n"
                + "[ ContainerRepository = " + containerRepository + " ]\n" + "[ ContainerTag = "
                + containerTag + " ]\n" + "[ CrawlerVersion = " + crawlerVersion + " ]\n"
                + "[ FetcherVersion = " + fetcherVersion + " ]\n" + "[ BlowoutVersion = "
                + blowoutVersion + " ]\n" + "[ FmaskVersion = " + fmaskVersion + " ]\n"
                + "[ CreationTime = " + creationTime + " ]\n" + "[ UpdateTime = " + updateTime
                + " ]\n" + "[ Status = " + status + " ]\n" + "[ Error = " + error + " ]\n"
                + "[ CollectionTierImageName = " + collectionTierName + " ]";
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("taskId", taskId);
        json.put("name", name);
        json.put("downloadLink", downloadLink);
        json.put("state", state.getValue());
        json.put("federationMember", federationMember);
        json.put("priority", priority);
        json.put("stationId", stationId);
        json.put("containerRepository", containerRepository);
        json.put("containerTag", containerTag);
        json.put("crawlerVersion", crawlerVersion);
        json.put("fetcherVersion", fetcherVersion);
        json.put("blowoutVersion", blowoutVersion);
        json.put("fmaskVersion", fmaskVersion);
        json.put("creationTime", creationTime);
        json.put("updateTime", updateTime);
        json.put("status", status);
        json.put("error", error);
        json.put("collectionTierName", collectionTierName);

        return json;
    }

    public Map<String, Integer> getTasksStatesCount() {
        return tasksStatesCount;
    }

    public void setTasksStatesCount(Map<String, Integer> tasksStatesCount) {
        this.tasksStatesCount = tasksStatesCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ImageTask) {
            ImageTask other = (ImageTask) o;
            return getTaskId().equals(other.getTaskId()) && getName().equals(other.getName())
                    && getDownloadLink().equals(other.getDownloadLink())
                    && getState().equals(other.getState()) && getPriority() == other.getPriority()
                    && getFederationMember().equals(other.getFederationMember())
                    && getStationId().equals(other.getStationId())
                    && getContainerRepository().equals(other.getContainerRepository())
                    && getContainerTag().equals(other.getContainerTag())
                    && getCrawlerVersion().equals(other.getCrawlerVersion())
                    && getFetcherVersion().equals(other.getFetcherVersion())
                    && getBlowoutVersion().equals(other.getBlowoutVersion())
                    && getFmaskVersion().equals(other.getFmaskVersion())
                    && getCreationTime().equals(other.getCreationTime())
                    && getUpdateTime().equals(other.getUpdateTime())
                    && getImageStatus().equals(other.getImageStatus())
                    && getImageError().equals(other.getImageError())
                    && getCollectionTierName().equals(other.getCollectionTierName());
        }
        return false;
    }
}
