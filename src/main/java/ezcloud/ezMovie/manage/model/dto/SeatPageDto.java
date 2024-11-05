package ezcloud.ezMovie.manage.model.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class SeatPageDto {
    private List<SeatDto> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;

    // Constructor
    public SeatPageDto(Page<SeatDto> page) {
        this.content = page.getContent();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.size = page.getSize();
        this.number = page.getNumber();
    }

    // Getters and setters
    public List<SeatDto> getContent() {
        return content;
    }

    public void setContent(List<SeatDto> content) {
        this.content = content;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
}
