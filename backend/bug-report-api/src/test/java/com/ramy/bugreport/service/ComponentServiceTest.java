package com.ramy.bugreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ramy.bugreport.domain.Component;
import com.ramy.bugreport.domain.UserAccount;
import com.ramy.bugreport.dto.component.CreateComponentRequest;
import com.ramy.bugreport.dto.component.ComponentResponse;
import com.ramy.bugreport.dto.component.UpdateComponentDescriptionRequest;
import com.ramy.bugreport.dto.component.UpdateComponentNameRequest;
import com.ramy.bugreport.dto.component.UpdateComponentResponsibleUserRequest;
import com.ramy.bugreport.exception.ResourceNotFoundException;
import com.ramy.bugreport.repository.IComponentRepository;
import com.ramy.bugreport.repository.ISoftwareProjectRepository;
import com.ramy.bugreport.repository.IUserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ComponentServiceTest {

    @Mock
    private IComponentRepository componentRepository;

    @Mock
    private IUserAccountRepository userAccountRepository;

    @Mock
    private ISoftwareProjectRepository softwareProjectRepository;

    private ComponentService service;

    @BeforeEach
    void setUp() {
        service = new ComponentService(componentRepository, userAccountRepository, softwareProjectRepository);
    }

    @Test
    void getAllMapsComponentsToResponsesWithResponsibleUserNames() {
        var componentId = UUID.randomUUID();
        var responsibleUserId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        var responsibleUser = org.mockito.Mockito.mock(UserAccount.class);
        when(componentRepository.findAll()).thenReturn(List.of(component));
        when(component.getId()).thenReturn(componentId);
        when(component.getProjectId()).thenReturn(projectId);
        when(component.getName()).thenReturn("API");
        when(component.getDescription()).thenReturn("Handles public endpoints");
        when(component.getResponsibleUserId()).thenReturn(responsibleUserId);
        when(userAccountRepository.findAllById(List.of(responsibleUserId)))
                .thenReturn(List.of(responsibleUser));
        when(responsibleUser.getId()).thenReturn(responsibleUserId);
        when(responsibleUser.getName()).thenReturn("Joe Responsible");

        var result = service.getAll(null);

        assertThat(result).containsExactly(new ComponentResponse(
                componentId, "API", "Handles public endpoints", "Joe Responsible", projectId));
        verify(componentRepository, never()).findByProjectId(any());
    }

    @Test
    void getAllWithProjectIdReturnsOnlyThatProjectsComponents() {
        var projectId = UUID.randomUUID();
        var component = new Component(UUID.randomUUID(), "API", null, null, projectId);
        when(softwareProjectRepository.existsById(projectId)).thenReturn(true);
        when(componentRepository.findByProjectId(projectId)).thenReturn(List.of(component));

        var result = service.getAll(projectId);

        assertThat(result).containsExactly(new ComponentResponse(
                component.getId(), "API", null, null, projectId));
        verify(componentRepository, never()).findAll();
    }

    @Test
    void getAllWithProjectIdThrowsWhenProjectDoesNotExist() {
        var projectId = UUID.randomUUID();
        when(softwareProjectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> service.getAll(projectId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project with id=%s does not exist!".formatted(projectId));
        verify(componentRepository, never()).findByProjectId(any());
    }

    @Test
    void createBuildsAndSavesComponentWithOptionalFields() {
        var responsibleUserId = UUID.randomUUID();
        var projectId = UUID.randomUUID();
        var request = new CreateComponentRequest(
                "API", projectId, "Handles public endpoints", responsibleUserId);
        var componentId = UUID.randomUUID();
        when(softwareProjectRepository.existsById(projectId)).thenReturn(true);
        when(userAccountRepository.existsById(responsibleUserId)).thenReturn(true);
        var savedComponent = org.mockito.Mockito.mock(Component.class);
        when(componentRepository.save(any(Component.class))).thenReturn(savedComponent);
        when(savedComponent.getId()).thenReturn(componentId);

        var result = service.create(request);

        var componentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentRepository).save(componentCaptor.capture());
        var component = componentCaptor.getValue();
        assertThat(component.getName()).isEqualTo(request.name());
        assertThat(component.getDescription()).isEqualTo(request.description());
        assertThat(component.getResponsibleUserId()).isEqualTo(request.responsibleUserId());
        assertThat(component.getProjectId()).isEqualTo(projectId);
        assertThat(result.id()).isEqualTo(componentId);
        assertThat(result.message()).isEqualTo("Successfully created!");
    }

    @Test
    void createAllowsMissingOptionalFields() {
        var projectId = UUID.randomUUID();
        var request = new CreateComponentRequest("API", projectId, null, null);
        var componentId = UUID.randomUUID();
        when(softwareProjectRepository.existsById(projectId)).thenReturn(true);
        var savedComponent = org.mockito.Mockito.mock(Component.class);
        when(componentRepository.save(any(Component.class))).thenReturn(savedComponent);
        when(savedComponent.getId()).thenReturn(componentId);

        var result = service.create(request);

        var componentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentRepository).save(componentCaptor.capture());
        assertThat(componentCaptor.getValue().getDescription()).isNull();
        assertThat(componentCaptor.getValue().getResponsibleUserId()).isNull();
        assertThat(result.id()).isEqualTo(componentId);
    }

    @Test
    void updateNameChangesTheLoadedComponent() {
        var componentId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        var request = new UpdateComponentNameRequest("Frontend");
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(component.getId()).thenReturn(componentId);

        var response = service.updateName(componentId, request);

        verify(component).setName("Frontend");
        assertThat(response.id()).isEqualTo(componentId);
        assertThat(response.message()).isEqualTo("Component updated successfully!");
    }

    @Test
    void updateDescriptionChangesTheLoadedComponent() {
        var componentId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        var request = new UpdateComponentDescriptionRequest("Handles the UI");
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(component.getId()).thenReturn(componentId);

        var response = service.updateDescription(componentId, request);

        verify(component).setDescription("Handles the UI");
        assertThat(response.id()).isEqualTo(componentId);
    }

    @Test
    void updateResponsibleUserIdChangesTheLoadedComponent() {
        var componentId = UUID.randomUUID();
        var responsibleUserId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        var request = new UpdateComponentResponsibleUserRequest(responsibleUserId);
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(userAccountRepository.existsById(responsibleUserId)).thenReturn(true);
        when(component.getId()).thenReturn(componentId);

        var response = service.updateResponsibleUserId(componentId, request);

        verify(component).setResponsibleUserId(responsibleUserId);
        assertThat(response.id()).isEqualTo(componentId);
    }

    @Test
    void createThrowsWhenResponsibleUserDoesNotExist() {
        var projectId = UUID.randomUUID();
        var responsibleUserId = UUID.randomUUID();
        when(softwareProjectRepository.existsById(projectId)).thenReturn(true);
        when(userAccountRepository.existsById(responsibleUserId)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.create(new CreateComponentRequest("API", projectId, null, responsibleUserId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id=%s does not exist!".formatted(responsibleUserId));
        org.mockito.Mockito.verify(componentRepository, org.mockito.Mockito.never()).save(any(Component.class));
    }

    @Test
    void createThrowsWhenProjectDoesNotExist() {
        var projectId = UUID.randomUUID();
        when(softwareProjectRepository.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateComponentRequest("API", projectId, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project with id=%s does not exist!".formatted(projectId));
        verify(componentRepository, never()).save(any(Component.class));
    }

    @Test
    void updateResponsibleUserIdThrowsWhenUserDoesNotExist() {
        var componentId = UUID.randomUUID();
        var responsibleUserId = UUID.randomUUID();
        var component = org.mockito.Mockito.mock(Component.class);
        when(componentRepository.findById(componentId)).thenReturn(Optional.of(component));
        when(userAccountRepository.existsById(responsibleUserId)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.updateResponsibleUserId(
                        componentId, new UpdateComponentResponsibleUserRequest(responsibleUserId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id=%s does not exist!".formatted(responsibleUserId));
        org.mockito.Mockito.verify(componentRepository, org.mockito.Mockito.never()).save(any(Component.class));
    }

    @Test
    void updateNameThrowsWhenComponentDoesNotExist() {
        var componentId = UUID.randomUUID();
        when(componentRepository.findById(componentId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.updateName(componentId, new UpdateComponentNameRequest("Frontend")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Component with id=%s does not exist!".formatted(componentId));
    }
}
