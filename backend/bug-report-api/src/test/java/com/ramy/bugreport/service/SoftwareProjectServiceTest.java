package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ramy.bugreport.domain.SoftwareProject;
import com.ramy.bugreport.dto.project.CreateSoftwareProjectRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectDescriptionRequest;
import com.ramy.bugreport.dto.project.UpdateSoftwareProjectNameRequest;
import com.ramy.bugreport.exception.ResourceNotFoundException;
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

    @Test
    void updateNameChangesTheLoadedProject() {
        var projectId = UUID.randomUUID();
        var project = org.mockito.Mockito.mock(SoftwareProject.class);
        var request = new UpdateSoftwareProjectNameRequest("Issue Tracker");
        when(softwareProjectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(project.getId()).thenReturn(projectId);

        var response = service.updateName(projectId, request);

        verify(project).setName("Issue Tracker");
        assertThat(response.id()).isEqualTo(projectId);
        assertThat(response.message()).isEqualTo("Project updated successfully!");
    }

    @Test
    void updateDescriptionChangesTheLoadedProject() {
        var projectId = UUID.randomUUID();
        var project = org.mockito.Mockito.mock(SoftwareProject.class);
        var request = new UpdateSoftwareProjectDescriptionRequest("Tracks reported issues");
        when(softwareProjectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(project.getId()).thenReturn(projectId);

        var response = service.updateDescription(projectId, request);

        verify(project).setDescription("Tracks reported issues");
        assertThat(response.id()).isEqualTo(projectId);
    }

    @Test
    void updateNameThrowsWhenProjectDoesNotExist() {
        var projectId = UUID.randomUUID();
        when(softwareProjectRepository.findById(projectId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.updateName(projectId, new UpdateSoftwareProjectNameRequest("Issue Tracker")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project with id=%s does not exist!".formatted(projectId));
    }
}
