package com.bernardoavila.slotly.resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository) {
        this.repository = repository;
    }

    public List<ResourceResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(ResourceService::toResponse)
                .toList();
    }

    public ResourceResponse findById(Long id) {
        Resource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        return toResponse(resource);
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = new Resource(request.name(), request.description());
        return toResponse(repository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        resource.rename(request.name(), request.description());
        return toResponse(resource);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private static ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getCreatedAt()
        );
    }
}