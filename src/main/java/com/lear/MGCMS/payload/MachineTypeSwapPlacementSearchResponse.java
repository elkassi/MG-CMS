package com.lear.MGCMS.payload;

import java.util.List;

public class MachineTypeSwapPlacementSearchResponse {

    private List<MachineTypeSwapPlacementDto> content;
    private int number;
    private int size;
    private int totalPages;
    private long totalElements;
    private int numberOfElements;
    private long totalPlacements;
    private boolean first;
    private boolean last;

    public MachineTypeSwapPlacementSearchResponse(
            List<MachineTypeSwapPlacementDto> content,
            int number,
            int size,
            int totalPages,
            long totalElements,
            long totalPlacements,
            boolean first,
            boolean last) {
        this.content = content;
        this.number = number;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.numberOfElements = content != null ? content.size() : 0;
        this.totalPlacements = totalPlacements;
        this.first = first;
        this.last = last;
    }

    public List<MachineTypeSwapPlacementDto> getContent() {
        return content;
    }

    public void setContent(List<MachineTypeSwapPlacementDto> content) {
        this.content = content;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    public long getTotalPlacements() {
        return totalPlacements;
    }

    public void setTotalPlacements(long totalPlacements) {
        this.totalPlacements = totalPlacements;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
