package com.example.smart_learning.permission;

import com.example.smart_learning.permission.dto.PermissionSnapshot;
import com.example.smart_learning.permission.entity.SysGroup;
import com.example.smart_learning.permission.entity.SysPriv;
import com.example.smart_learning.permission.repository.SysGroupPrivRepository;
import com.example.smart_learning.permission.repository.SysGroupRepository;
import com.example.smart_learning.permission.repository.SysPrivRepository;
import com.example.smart_learning.permission.repository.SysUserGroupRepository;
import com.example.smart_learning.permission.service.PermissionService;
import com.example.smart_learning.user.entity.SysUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionServiceTest {

    private final SysUserGroupRepository userGroupRepository = mock(SysUserGroupRepository.class);
    private final SysGroupRepository groupRepository = mock(SysGroupRepository.class);
    private final SysGroupPrivRepository groupPrivRepository = mock(SysGroupPrivRepository.class);
    private final SysPrivRepository privRepository = mock(SysPrivRepository.class);
    private final PermissionService service = new PermissionService(
            userGroupRepository, groupRepository, groupPrivRepository, privRepository);

    @Test
    void multipleGroupsUnionRightsAndUseHighestDataScope() {
        SysUser user = user(7L, 3L);
        when(userGroupRepository.findActiveGroupIdsByUserId(7L)).thenReturn(List.of(1L, 2L));
        when(groupRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                group(1L, "TEACHER", "OWN", true),
                group(2L, "STUDENT", "SELF", false)));
        when(groupPrivRepository.findPrivIdsByGroupIds(List.of(1L, 2L))).thenReturn(List.of(10L, 11L));
        when(privRepository.findByIdIn(anyList())).thenReturn(List.of(
                priv(10L, "course", "create", "ACTION"),
                priv(11L, "course", "browse", "VIEW")));

        PermissionSnapshot snapshot = service.getSnapshot(user);

        assertThat(snapshot.rights().get("course")).containsExactlyInAnyOrder("create", "browse");
        assertThat(snapshot.dataScope()).isEqualTo("OWN");
        assertThat(snapshot.limited()).isFalse();
        assertThat(snapshot.menus()).hasSize(1);
        assertThat(snapshot.menus().get(0).path()).isEqualTo("/courses");
        assertThat(snapshot.groupCodes()).containsExactlyInAnyOrder("TEACHER", "STUDENT");
    }

    @Test
    void userWithOnlyLimitedGroupsIsLimited() {
        SysUser user = user(8L, 1L);
        when(userGroupRepository.findActiveGroupIdsByUserId(8L)).thenReturn(List.of(1L, 2L));
        when(groupRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                group(1L, "LIMITED_A", "OWN", true),
                group(2L, "LIMITED_B", "SELF", true)));
        when(groupPrivRepository.findPrivIdsByGroupIds(List.of(1L, 2L))).thenReturn(List.of(10L));
        when(privRepository.findByIdIn(anyList())).thenReturn(List.of(priv(10L, "course", "view", "VIEW")));

        PermissionSnapshot snapshot = service.getSnapshot(user);

        assertThat(snapshot.limited()).isTrue();
        assertThat(snapshot.dataScope()).isEqualTo("OWN");
    }

    @Test
    void userWithoutGroupsHasNoRights() {
        SysUser user = user(9L, 0L);
        when(userGroupRepository.findActiveGroupIdsByUserId(9L)).thenReturn(List.of());

        PermissionSnapshot snapshot = service.getSnapshot(user);

        assertThat(snapshot.rights()).isEmpty();
        assertThat(snapshot.menus()).isEmpty();
        assertThat(snapshot.limited()).isTrue();
    }

    private SysUser user(Long id, Long privVersion) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setAccount("user" + id);
        user.setPrivVersion(privVersion);
        return user;
    }

    private SysGroup group(Long id, String code, String dataScope, boolean limited) {
        SysGroup group = new SysGroup();
        group.setId(id);
        group.setCode(code);
        group.setName(code);
        group.setDataScope(dataScope);
        group.setLimited(limited);
        group.setSort((int) (10 * id));
        return group;
    }

    private SysPriv priv(Long id, String module, String method, String type) {
        SysPriv priv = new SysPriv();
        priv.setId(id);
        priv.setModule(module);
        priv.setMethod(method);
        priv.setName(module + "/" + method);
        priv.setType(type);
        priv.setMenuPath(type.equals("VIEW") ? "/courses" : null);
        priv.setSort(10);
        priv.setStatus("ACTIVE");
        return priv;
    }
}
