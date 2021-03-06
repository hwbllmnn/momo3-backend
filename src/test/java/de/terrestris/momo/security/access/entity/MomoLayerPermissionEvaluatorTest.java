/**
 *
 */
package de.terrestris.momo.security.access.entity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.terrestris.momo.dao.MomoApplicationDao;
import de.terrestris.momo.dao.UserGroupRoleDao;
import de.terrestris.momo.model.MomoApplication;
import de.terrestris.momo.model.MomoLayer;
import de.terrestris.momo.model.MomoUser;
import de.terrestris.momo.model.MomoUserGroup;
import de.terrestris.momo.model.security.UserGroupRole;
import de.terrestris.momo.service.MomoApplicationService;
import de.terrestris.momo.service.UserGroupRoleService;
import de.terrestris.momo.util.config.MomoConfigHolder;
import de.terrestris.momo.util.security.MomoSecurityUtil;
import de.terrestris.shogun2.helper.IdHelper;
import de.terrestris.shogun2.model.Role;
import de.terrestris.shogun2.model.User;
import de.terrestris.shogun2.model.UserGroup;
import de.terrestris.shogun2.model.security.Permission;
import de.terrestris.shogun2.model.security.PermissionCollection;

/**
 *
 * @author Daniel Koch
 * @author Andre Henn
 * @author terrestris GmbH & Co. KG
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath*:META-INF/spring/test-user-group-roles.xml"
})
public class MomoLayerPermissionEvaluatorTest {

	@InjectMocks
	private MomoLayerPermissionEvaluator<MomoLayer> momoLayerPermissionEvaluator;

	@Value("${role.defaultUserRoleName:}")
	private String defaultUserRoleName;

	@Value("${role.editorRoleName:}")
	private String editorRoleName;

	@Value("${role.subAdminRoleName:}")
	private String subAdminRoleName;

	@Value("${role.superAdminRoleName:}")
	private String superAdminRoleName;

	/**
	 *
	 */
	private MomoUser accessUser;
	private MomoLayer testLayer;
	private MomoUserGroup testGroup;

	@Before
	public void set_up() throws NoSuchFieldException, IllegalAccessException {
		MockitoAnnotations.initMocks(this);

		MomoConfigHolder momoConfigHolder = Mockito.mock(MomoConfigHolder.class);
		when(momoConfigHolder.getDefaultUserRoleName()).thenReturn(defaultUserRoleName);
		when(momoConfigHolder.getEditorRoleName()).thenReturn(editorRoleName);
		when(momoConfigHolder.getSubAdminRoleName()).thenReturn(subAdminRoleName);
		when(momoConfigHolder.getSuperAdminRoleName()).thenReturn(superAdminRoleName);

		MomoSecurityUtil.configHolder = momoConfigHolder;

		momoLayerPermissionEvaluator = new MomoLayerPermissionEvaluator<MomoLayer>();

		// A User that wants to access the application.
		accessUser = new MomoUser();
		accessUser.setAccountName("Shinji");
		IdHelper.setIdOnPersistentObject(accessUser, 1909);

		// A Group that wants to access the application.
		testGroup = new MomoUserGroup();
		testGroup.setName("BVB");
		IdHelper.setIdOnPersistentObject(testGroup, 19);

		testLayer = new MomoLayer();
		IdHelper.setIdOnPersistentObject(testLayer, 191909);
	}

	@After
	public void clean_up() {
		logoutMockUser();
	}

	/**
	 *
	 * @param userRoles
	 */
	private void loginMockUser(Set<Role> userRoles) {
		Set<GrantedAuthority> grantedAuthorities = new HashSet<GrantedAuthority>();

		for (Role userRole : userRoles) {
			grantedAuthorities.add(new SimpleGrantedAuthority(userRole.getName()));
		}

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				accessUser, "", grantedAuthorities);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	/**
	 *
	 */
	private void logoutMockUser() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void hasPermission_shouldDenyWithoutUserNorGroupPermissions() throws NoSuchFieldException, IllegalAccessException {
		Set<Permission> permissionsToCheck = new HashSet<Permission>();
		permissionsToCheck.add(Permission.READ);
		permissionsToCheck.add(Permission.CREATE);
		permissionsToCheck.add(Permission.UPDATE);
		permissionsToCheck.add(Permission.DELETE);

		MomoApplicationService<MomoApplication, MomoApplicationDao<MomoApplication>> momoApplicationServiceMock =
				Mockito.mock(MomoApplicationService.class);
		when(momoApplicationServiceMock.findAll()).thenReturn(new ArrayList<MomoApplication>());
		momoLayerPermissionEvaluator.setMomoApplicationService(momoApplicationServiceMock);

		for (Permission permission : permissionsToCheck) {
			boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, testLayer, permission);
			assertFalse(permissionResult);
		}
	}

	@Test
	public void hasPermission_shouldAllowReadForUserGrantedFromUserPermissions() throws NoSuchFieldException, IllegalAccessException {
		Set<Permission> readPermission = new HashSet<Permission>();
		readPermission.add(Permission.READ);

		PermissionCollection permCollection = new PermissionCollection();
		permCollection.setPermissions(readPermission);
		IdHelper.setIdOnPersistentObject(permCollection, 333);

		HashMap<User, PermissionCollection> userPermissions = new HashMap<User, PermissionCollection>();
		userPermissions.put(accessUser, permCollection);
		testLayer.setUserPermissions(userPermissions);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, testLayer, Permission.READ);
		assertTrue(permissionResult);
	}

	@Test
	public void hasPermission_shouldAllowUpdateForUserGrantedFromUserPermissions() throws NoSuchFieldException, IllegalAccessException {
		Set<Permission> updatePermission = new HashSet<Permission>();
		updatePermission.add(Permission.UPDATE);

		PermissionCollection permCollection = new PermissionCollection();
		permCollection.setPermissions(updatePermission);
		IdHelper.setIdOnPersistentObject(permCollection, 333);

		HashMap<User, PermissionCollection> userPermissions = new HashMap<User, PermissionCollection>();
		userPermissions.put(accessUser, permCollection);
		testLayer.setUserPermissions(userPermissions);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, testLayer, Permission.UPDATE);
		assertTrue(permissionResult);
	}

	@Test
	public void hasPermission_shouldAllowDeleteForUserGrantedFromUserPermissions() throws NoSuchFieldException, IllegalAccessException {
		Set<Permission> deletePermission = new HashSet<Permission>();
		deletePermission.add(Permission.DELETE);

		PermissionCollection permCollection = new PermissionCollection();
		permCollection.setPermissions(deletePermission);
		IdHelper.setIdOnPersistentObject(permCollection, 333);

		HashMap<User, PermissionCollection> userPermissions = new HashMap<User, PermissionCollection>();
		userPermissions.put(accessUser, permCollection);
		testLayer.setUserPermissions(userPermissions);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, testLayer, Permission.DELETE);
		assertTrue(permissionResult);
	}

	@Test
	public void hasPermission_shouldAllowDeleteForUserGrantedFromGroupPermissions() throws NoSuchFieldException, IllegalAccessException {
		Set<Permission> readPermission = new HashSet<Permission>();
		readPermission.add(Permission.DELETE);

		PermissionCollection permCollection = new PermissionCollection();
		permCollection.setPermissions(readPermission);
		IdHelper.setIdOnPersistentObject(permCollection, 333);

		HashMap<UserGroup, PermissionCollection> groupPermissions = new HashMap<UserGroup, PermissionCollection>();
		groupPermissions.put(testGroup, permCollection);
		testLayer.setGroupPermissions(groupPermissions);

		UserGroupRoleService<UserGroupRole, UserGroupRoleDao<UserGroupRole>> userGroupRoleService =
				Mockito.mock(UserGroupRoleService.class);
		when(userGroupRoleService.isUserMemberInUserGroup(accessUser, testGroup)).thenReturn(true);
		momoLayerPermissionEvaluator.setUserGroupRoleService(userGroupRoleService);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, testLayer, Permission.DELETE);
		assertTrue(permissionResult);
	}

	@Test
	public void hasPermission_shouldAllowUpdateForUserGrantedFromGroupPermissions() throws NoSuchFieldException, IllegalAccessException {
		Set<Permission> readPermission = new HashSet<Permission>();
		readPermission.add(Permission.UPDATE);

		PermissionCollection permCollection = new PermissionCollection();
		permCollection.setPermissions(readPermission);
		IdHelper.setIdOnPersistentObject(permCollection, 333);

		HashMap<UserGroup, PermissionCollection> groupPermissions = new HashMap<UserGroup, PermissionCollection>();
		groupPermissions.put(testGroup, permCollection);
		testLayer.setGroupPermissions(groupPermissions);

		UserGroupRoleService<UserGroupRole, UserGroupRoleDao<UserGroupRole>> userGroupRoleService =
				Mockito.mock(UserGroupRoleService.class);
		when(userGroupRoleService.isUserMemberInUserGroup(accessUser, testGroup)).thenReturn(true);
		momoLayerPermissionEvaluator.setUserGroupRoleService(userGroupRoleService);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, testLayer, Permission.UPDATE);
		assertTrue(permissionResult);
	}

	@Test
	public void hasPermission_shouldDenyCreateForDefaultUser() throws NoSuchFieldException, IllegalAccessException {
		// prepare a user that
		final Role role = new Role(defaultUserRoleName);
		IdHelper.setIdOnPersistentObject(role, 44);

		Set<Role> userRoles = new HashSet<Role>();
		userRoles.add(role);

		loginMockUser(userRoles);

		MomoApplicationService<MomoApplication, MomoApplicationDao<MomoApplication>> momoApplicationServiceMock =
				Mockito.mock(MomoApplicationService.class);
		when(momoApplicationServiceMock.findAll()).thenReturn(new ArrayList<MomoApplication>());
		momoLayerPermissionEvaluator.setMomoApplicationService(momoApplicationServiceMock);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, null, Permission.CREATE);
		assertFalse(permissionResult);
	}

	@Test
	public void hasPermission_shouldAllowCreateForEditorUser() throws NoSuchFieldException, IllegalAccessException {
		// prepare a user that
		final Role role = new Role(editorRoleName);
		IdHelper.setIdOnPersistentObject(role, 44);

		Set<Role> userRoles = new HashSet<Role>();
		userRoles.add(role);

		loginMockUser(userRoles);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, null, Permission.CREATE);
		assertTrue(permissionResult);
	}

	@Test
	public void hasPermission_shouldAllowCreateForSubAdminUser() throws NoSuchFieldException, IllegalAccessException {
		// prepare a user that
		final Role role = new Role(subAdminRoleName);
		IdHelper.setIdOnPersistentObject(role, 44);

		Set<Role> userRoles = new HashSet<Role>();
		userRoles.add(role);

		loginMockUser(userRoles);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, null, Permission.CREATE);
		assertTrue(permissionResult);
	}

	@Test
	public void hasPermission_shouldAllowReadForUserGrantedFromGroupPermissions() throws NoSuchFieldException, IllegalAccessException {
		Set<Permission> readPermission = new HashSet<Permission>();
		readPermission.add(Permission.READ);

		PermissionCollection permCollection = new PermissionCollection();
		permCollection.setPermissions(readPermission);
		IdHelper.setIdOnPersistentObject(permCollection, 333);

		HashMap<UserGroup, PermissionCollection> groupPermissions = new HashMap<UserGroup, PermissionCollection>();
		groupPermissions.put(testGroup, permCollection);
		testLayer.setGroupPermissions(groupPermissions);

		UserGroupRoleService<UserGroupRole, UserGroupRoleDao<UserGroupRole>> userGroupRoleService =
				Mockito.mock(UserGroupRoleService.class);
		when(userGroupRoleService.isUserMemberInUserGroup(accessUser, testGroup)).thenReturn(true);
		momoLayerPermissionEvaluator.setUserGroupRoleService(userGroupRoleService);

		boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, testLayer, Permission.READ);
		assertTrue(permissionResult);
	}

	/**
	 * Always allow CRUD for owned Layer..
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	@Test
	public void hasPermission_shouldAllowCrudForUserOwningLayerWithEmptyUserPermissionsAndEmptyGroupPermissions() throws NoSuchFieldException, IllegalAccessException {
//		HashMap<UserGroup, PermissionCollection> emptyGroupPermissions = new HashMap<UserGroup, PermissionCollection>();
//		testLayer.setGroupPermissions(emptyGroupPermissions);
//
//		HashMap<User, PermissionCollection> emptyUserPermissions = new HashMap<User, PermissionCollection>();
//		testLayer.setUserPermissions(emptyUserPermissions);
//
//		testLayer.setOwner(accessUser);
//
//		Set<Permission> permissionsToCheck = new HashSet<Permission>();
//		permissionsToCheck.add(Permission.READ);
//		permissionsToCheck.add(Permission.CREATE);
//		permissionsToCheck.add(Permission.UPDATE);
//		permissionsToCheck.add(Permission.DELETE);
//
//		for (Permission permission : permissionsToCheck) {
//			boolean permissionResult = momoLayerPermissionEvaluator.hasPermission(accessUser, testLayer, permission);
//			assertTrue(permissionResult);
//		}
	}

}
