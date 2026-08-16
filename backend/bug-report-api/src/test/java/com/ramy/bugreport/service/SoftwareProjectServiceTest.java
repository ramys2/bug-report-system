package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectRequest;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;

@ExtendWith(MockitoExtension.class)
class SoftwareProjectServiceTest {

    @Mock
    private ISoftwareProjectRepository softwareProjectRepository;

    private SoftwareProjectService service;

    @BeforeEach
    void setUp() {
        service = new SoftwareProjectService(softwareProjectRepository);
    }

    @Test
    void getAllMapsProjectIdsToNames() {
        var projectId = UUID.randomUUID();
        var project = org.mockito.Mockito.mock(SoftwareProject.class);
        when(softwareProjectRepository.findAll()).thenReturn(List.of(project));
        when(project.getId()).thenReturn(projectId);
        when(project.getName()).thenReturn("Bug Report System");

        var result = service.getAll();

        assertThat(result).containsExactlyEntriesOf(Map.of(projectId, "Bug Report System"));
    }

    @Test
    void createBuildsAndSavesProjectWithOptionalDescription() {
        var request = new CreateSoftwareProjectRequest("Bug Report System", null);
        var projectId = UUID.randomUUID();
        var savedProject = org.mockito.Mockito.mock(SoftwareProject.class);
        when(softwareProjectRepository.save(any(SoftwareProject.class))).thenReturn(savedProject);
        when(savedProject.getId()).thenReturn(projectId);

        var result = service.create(request);

        var projectCaptor = ArgumentCaptor.forClass(SoftwareProject.class);
        verify(softwareProjectRepository).save(projectCaptor.capture());
        var project = projectCaptor.getValue();
        assertThat(project.getName()).isEqualTo(request.name());
        assertThat(project.getDescription()).isEqualTo(request.description());
        assertThat(result.id()).isEqualTo(projectId);
        assertThat(result.message()).isEqualTo("Successfully created!");
    }
}
