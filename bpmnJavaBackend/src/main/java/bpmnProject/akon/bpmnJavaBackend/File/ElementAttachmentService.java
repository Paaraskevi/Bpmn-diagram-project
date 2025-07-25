package bpmnProject.akon.bpmnJavaBackend.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ElementAttachmentService {

    private final ElementAttachmentRepository elementAttachmentRepository;
    private final FileRepository fileRepository;

    @Autowired
    public ElementAttachmentService(ElementAttachmentRepository elementAttachmentRepository,
                                    FileRepository fileRepository) {
        this.elementAttachmentRepository = elementAttachmentRepository;
        this.fileRepository = fileRepository;
    }

    /**
     * Add attachment to BPMN element
     */
    @Transactional
    public ElementAttachment addElementAttachment(Long parentFileId, String elementId,
                                                  String elementType, MultipartFile file,
                                                  String description, String createdBy) throws IOException {
        File parentFile = fileRepository.findById(parentFileId)
                .orElseThrow(() -> new RuntimeException("Parent file not found with id: " + parentFileId));

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("Attachment file is empty");
        }

        // Create attachment
        ElementAttachment attachment = new ElementAttachment();
        attachment.setParentFile(parentFile);
        attachment.setElementId(elementId);
        attachment.setElementType(elementType);
        attachment.setAttachmentName(generateAttachmentName(file.getOriginalFilename(), elementId));
        attachment.setOriginalFilename(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setDescription(description);
        attachment.setCreatedBy(createdBy);
        attachment.setCreatedTime(LocalDateTime.now());
        attachment.setAttachmentData(file.getBytes());
        attachment.setCategory(determineCategory(file));
        attachment.setIsPublic(false);
        attachment.setIsDownloadable(true);

        ElementAttachment savedAttachment = elementAttachmentRepository.save(attachment);

        // Update parent file's updated time
        parentFile.setUpdatedTime(LocalDateTime.now());
        parentFile.setUpdatedBy(createdBy);
        fileRepository.save(parentFile);

        return savedAttachment;
    }

    /**
     * Get all attachments for a specific element
     */
    @Transactional(readOnly = true)
    public List<ElementAttachment> getElementAttachments(Long parentFileId, String elementId) {
        return elementAttachmentRepository.findAttachmentsForElement(parentFileId, elementId);
    }

    /**
     * Get all attachments for a file
     */
    @Transactional(readOnly = true)
    public List<ElementAttachment> getFileAttachments(Long parentFileId) {
        return elementAttachmentRepository.findByParentFileId(parentFileId);
    }

    /**
     * Get attachment by id
     */
    @Transactional(readOnly = true)
    public Optional<ElementAttachment> getAttachment(Long attachmentId) {
        return elementAttachmentRepository.findById(attachmentId);
    }

    /**
     * Update attachment metadata
     */
    @Transactional
    public ElementAttachment updateAttachment(Long attachmentId, String newName,
                                              String newDescription, String updatedBy) {
        ElementAttachment attachment = elementAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));

        attachment.setAttachmentName(newName);
        attachment.setDescription(newDescription);
        attachment.setUpdatedBy(updatedBy);
        attachment.setUpdatedTime(LocalDateTime.now());

        return elementAttachmentRepository.save(attachment);
    }

    /**
     * Replace attachment file
     */
    @Transactional
    public ElementAttachment replaceAttachmentFile(Long attachmentId, MultipartFile newFile,
                                                   String updatedBy) throws IOException {
        ElementAttachment attachment = elementAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));

        if (newFile.isEmpty()) {
            throw new RuntimeException("New attachment file is empty");
        }

        // Update file data
        attachment.setOriginalFilename(newFile.getOriginalFilename());
        attachment.setFileType(newFile.getContentType());
        attachment.setFileSize(newFile.getSize());
        attachment.setAttachmentData(newFile.getBytes());
        attachment.setCategory(determineCategory(newFile));
        attachment.setUpdatedBy(updatedBy);
        attachment.setUpdatedTime(LocalDateTime.now());

        return elementAttachmentRepository.save(attachment);
    }

    /**
     * Delete attachment
     */
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        ElementAttachment attachment = elementAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));

        // Update parent file's updated time
        File parentFile = attachment.getParentFile();
        parentFile.setUpdatedTime(LocalDateTime.now());
        fileRepository.save(parentFile);

        elementAttachmentRepository.delete(attachment);
    }

    /**
     * Delete all attachments for an element
     */
    @Transactional
    public void deleteElementAttachments(Long parentFileId, String elementId) {
        elementAttachmentRepository.deleteByParentFileIdAndElementId(parentFileId, elementId);
    }

    /**
     * Search attachments
     */
    @Transactional(readOnly = true)
    public List<ElementAttachment> searchAttachments(String searchTerm) {
        return elementAttachmentRepository.searchByName(searchTerm);
    }

    /**
     * Get attachments by category
     */
    @Transactional(readOnly = true)
    public List<ElementAttachment> getAttachmentsByCategory(ElementAttachment.AttachmentCategory category) {
        return elementAttachmentRepository.findByCategory(category);
    }

    /**
     * Get attachment count for element
     */
    @Transactional(readOnly = true)
    public Long getElementAttachmentCount(Long parentFileId, String elementId) {
        return elementAttachmentRepository.countByParentFileIdAndElementId(parentFileId, elementId);
    }

    /**
     * Get attachment statistics for file
     */
    @Transactional(readOnly = true)
    public AttachmentStatistics getFileAttachmentStatistics(Long parentFileId) {
        List<ElementAttachment> attachments = elementAttachmentRepository.findByParentFileId(parentFileId);

        long totalSize = attachments.stream()
                .mapToLong(a -> a.getFileSize() != null ? a.getFileSize() : 0L)
                .sum();

        long imageCount = attachments.stream()
                .filter(ElementAttachment::isImageFile)
                .count();

        long documentCount = attachments.stream()
                .filter(a -> !a.isImageFile() && !a.isPdfFile())
                .count();

        long pdfCount = attachments.stream()
                .filter(ElementAttachment::isPdfFile)
                .count();

        AttachmentStatistics stats = new AttachmentStatistics();
        stats.setTotalAttachments(attachments.size());
        stats.setTotalSize(totalSize);
        stats.setImageCount(imageCount);
        stats.setDocumentCount(documentCount);
        stats.setPdfCount(pdfCount);
        stats.setAverageSize(attachments.isEmpty() ? 0 : totalSize / attachments.size());

        return stats;
    }

    /**
     * Copy attachments from one element to another
     */
    @Transactional
    public List<ElementAttachment> copyElementAttachments(Long sourceFileId, String sourceElementId,
                                                          Long targetFileId, String targetElementId,
                                                          String copiedBy) {
        List<ElementAttachment> sourceAttachments = getElementAttachments(sourceFileId, sourceElementId);
        File targetFile = fileRepository.findById(targetFileId)
                .orElseThrow(() -> new RuntimeException("Target file not found with id: " + targetFileId));

        List<ElementAttachment> copiedAttachments = new java.util.ArrayList<>();

        for (ElementAttachment source : sourceAttachments) {
            ElementAttachment copy = new ElementAttachment();
            copy.setParentFile(targetFile);
            copy.setElementId(targetElementId);
            copy.setElementType(source.getElementType());
            copy.setAttachmentName("Copy of " + source.getAttachmentName());
            copy.setOriginalFilename(source.getOriginalFilename());
            copy.setFileType(source.getFileType());
            copy.setFileSize(source.getFileSize());
            copy.setDescription("Copied from element " + sourceElementId + ": " + source.getDescription());
            copy.setCreatedBy(copiedBy);
            copy.setCreatedTime(LocalDateTime.now());
            copy.setAttachmentData(source.getAttachmentData());
            copy.setCategory(source.getCategory());
            copy.setIsPublic(source.getIsPublic());
            copy.setIsDownloadable(source.getIsDownloadable());

            copiedAttachments.add(elementAttachmentRepository.save(copy));
        }

        return copiedAttachments;
    }

    /**
     * Generate unique attachment name
     */
    private String generateAttachmentName(String originalFilename, String elementId) {
        if (originalFilename == null) {
            return "attachment_" + elementId + "_" + System.currentTimeMillis();
        }

        String name = originalFilename;
        String extension = "";

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            name = originalFilename.substring(0, lastDotIndex);
            extension = originalFilename.substring(lastDotIndex);
        }

        return name + "_" + elementId + extension;
    }

    /**
     * Determine attachment category based on file type
     */
    private ElementAttachment.AttachmentCategory determineCategory(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return ElementAttachment.AttachmentCategory.IMAGE;
            } else if (contentType.equals("application/pdf")) {
                return ElementAttachment.AttachmentCategory.DOCUMENT;
            } else if (contentType.startsWith("text/") ||
                    contentType.equals("application/msword") ||
                    contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                return ElementAttachment.AttachmentCategory.DOCUMENT;
            }
        }

        if (filename != null) {
            String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp":
                    return ElementAttachment.AttachmentCategory.IMAGE;
                case "pdf", "doc", "docx", "txt", "rtf":
                    return ElementAttachment.AttachmentCategory.DOCUMENT;
                case "xls", "xlsx", "csv":
                    return ElementAttachment.AttachmentCategory.DOCUMENT;
                case "ppt", "pptx":
                    return ElementAttachment.AttachmentCategory.DOCUMENT;
                default:
                    return ElementAttachment.AttachmentCategory.OTHER;
            }
        }

        return ElementAttachment.AttachmentCategory.OTHER;
    }

    // =================== RESPONSE CLASSES ===================

    public static class AttachmentStatistics {
        private Integer totalAttachments;
        private Long totalSize;
        private Long imageCount;
        private Long documentCount;
        private Long pdfCount;
        private Long averageSize;

        public AttachmentStatistics() {}

        public Integer getTotalAttachments() { return totalAttachments; }
        public void setTotalAttachments(Integer totalAttachments) { this.totalAttachments = totalAttachments; }

        public Long getTotalSize() { return totalSize; }
        public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }

        public Long getImageCount() { return imageCount; }
        public void setImageCount(Long imageCount) { this.imageCount = imageCount; }

        public Long getDocumentCount() { return documentCount; }
        public void setDocumentCount(Long documentCount) { this.documentCount = documentCount; }

        public Long getPdfCount() { return pdfCount; }
        public void setPdfCount(Long pdfCount) { this.pdfCount = pdfCount; }

        public Long getAverageSize() { return averageSize; }
        public void setAverageSize(Long averageSize) { this.averageSize = averageSize; }
    }
}