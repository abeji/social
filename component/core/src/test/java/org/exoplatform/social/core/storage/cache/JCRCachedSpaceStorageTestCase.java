package org.exoplatform.social.core.storage.cache;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.SpaceStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.impl.IdentityStorageImpl;
import org.exoplatform.social.core.test.AbstractCoreTest;
import org.exoplatform.social.core.test.MaxQueryNumber;
import org.exoplatform.social.core.test.QueryNumberTest;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
@QueryNumberTest
public class JCRCachedSpaceStorageTestCase extends AbstractCoreTest {

  private JCRCachedSpaceStorage cachedSpaceStorage;
  private SocialStorageCacheService cacheService;
  private JCRCachedActivityStorage cachedActivityStorage;
  private IdentityStorage identityStorage;
  
  private Identity demo;
  private Identity john;
  private Identity mary;
  private Identity root;
  
  private List<Space>  tearDownSpaceList;
  private List<ExoSocialActivity>  tearDownActivityList;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    begin();

    cachedSpaceStorage = getContainer().getComponentInstanceOfType(JCRCachedSpaceStorage.class);
    cachedActivityStorage = getContainer().getComponentInstanceOfType(JCRCachedActivityStorage.class);
    identityStorage = getContainer().getComponentInstanceOfType(IdentityStorageImpl.class);
    cacheService = getContainer().getComponentInstanceOfType(SocialStorageCacheService.class);
    
    
    demo = new Identity(OrganizationIdentityProvider.NAME, "demo");
    mary = new Identity(OrganizationIdentityProvider.NAME, "mary");
    john = new Identity(OrganizationIdentityProvider.NAME, "john");
    root = new Identity(OrganizationIdentityProvider.NAME, "root");
    

    identityStorage.saveIdentity(demo);
    identityStorage.saveIdentity(mary);
    identityStorage.saveIdentity(john);
    identityStorage.saveIdentity(root);
    
    tearDownSpaceList = new ArrayList<Space>();
    tearDownActivityList = new ArrayList<ExoSocialActivity>();
  }
  
  @Override
  public void tearDown() throws Exception {
    
    for (Space sp : tearDownSpaceList) {
      try {
        cachedSpaceStorage.deleteSpace(sp.getId());
      } catch (SpaceStorageException e) {
        // Could be expected if already deleted in test
      }
    }
    
    for (ExoSocialActivity act : tearDownActivityList) {
      try {
        cachedActivityStorage.deleteActivity(act.getId());
      } catch (ActivityStorageException e) {
        // Could be expected if already deleted in test
      }
    }
    
    identityStorage.deleteIdentity(demo);
    identityStorage.deleteIdentity(mary);
    identityStorage.deleteIdentity(john);
    identityStorage.deleteIdentity(root);
    
    end();
    super.tearDown();
  }


  @MaxQueryNumber(225)
  public void testRemoveSpace() throws Exception {

    //
    Space space = new Space();
    space.setDisplayName("Hello");
    space.setPrettyName(space.getDisplayName());
    cachedSpaceStorage.saveSpace(space, true);
    tearDownSpaceList.add(space);

    //
    Identity i = new Identity("foo", "bar");
    identityStorage.saveIdentity(i);

    //
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("hello");
    activity.setUserId(i.getId());
    cachedActivityStorage.saveActivity(i, activity);
    
    //
    List<Identity> is = new ArrayList<Identity>();
    is.add(i);
    assertEquals(0, cacheService.getActivitiesCache().getCacheSize());
    cachedActivityStorage.getActivitiesOfIdentities(is, 0, 10).size();
    assertEquals(1, cacheService.getActivitiesCache().getCacheSize());
    
    //
    cachedSpaceStorage.deleteSpace(space.getId());
    assertEquals(0, cacheService.getActivitiesCache().getCacheSize());

  }
  
  /**
   * Test {@link CachedSpaceStorage#renameSpace(Space, String)}
   * 
   * @throws Exception
   * @since 1.2.8
   */
  @MaxQueryNumber(960)
  public void testRenameSpace() throws Exception {
    Space space = new Space();
    space.setDisplayName("Hello");
    space.setPrettyName(space.getDisplayName());
    space.setGroupId("/space/Hello");
    space.setUrl(space.getPrettyName());
    String[] managers = new String[] {"demo", "mary"};
    String[] members = new String[] {"demo", "mary", "john"};
    space.setManagers(managers);
    space.setMembers(members);
    cachedSpaceStorage.saveSpace(space, true);
    tearDownSpaceList.add(space);

    //
    Identity identitySpace = new Identity(SpaceIdentityProvider.NAME, space.getPrettyName());
    identityStorage.saveIdentity(identitySpace);
    identitySpace = identityStorage.findIdentity(SpaceIdentityProvider.NAME, space.getPrettyName());

    //
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("hello");
    activity.setUserId(identitySpace.getId());
    cachedActivityStorage.saveActivity(identitySpace, activity);
    
    //
    List<Identity> is = new ArrayList<Identity>();
    is.add(identitySpace);
    assertEquals(0, cacheService.getActivitiesCache().getCacheSize());
    cachedActivityStorage.getActivitiesOfIdentities(is, 0, 10).size();
    assertEquals(1, cacheService.getActivitiesCache().getCacheSize());
    
    String newDisplayName = "new display name";
    
    cachedSpaceStorage.renameSpace(space, newDisplayName);
    
    Space got = cachedSpaceStorage.getSpaceById(space.getId());
    assertEquals(newDisplayName, got.getDisplayName());
    
    assertEquals(0, cacheService.getActivitiesCache().getCacheSize());
    assertEquals(0, cacheService.getIdentitiesCache().getCacheSize());
    
    //
    cachedSpaceStorage.deleteSpace(space.getId());
    assertEquals(0, cacheService.getActivitiesCache().getCacheSize());
  }
  
  /**
   * Test {@link CachedSpaceStorage#renameSpace(Space, String)}
   * 
   * @throws Exception
   * @since 4.1-RC1
   */
  @MaxQueryNumber(1335)
  public void testRenameSpaceOnCache() throws Exception {
    Space space = new Space();
    space.setDisplayName("Social");
    space.setPrettyName(space.getDisplayName());
    space.setGroupId("/space/Social");
    space.setUrl(space.getPrettyName());
    String[] managers = new String[] {"demo", "mary"};
    String[] members = new String[] {"demo", "mary", "john"};
    space.setManagers(managers);
    space.setMembers(members);
    cachedSpaceStorage.saveSpace(space, true);
    tearDownSpaceList.add(space);

    //
    Identity identitySpace = new Identity(SpaceIdentityProvider.NAME, space.getPrettyName());
    identityStorage.saveIdentity(identitySpace);
    identitySpace = identityStorage.findIdentity(SpaceIdentityProvider.NAME, space.getPrettyName());

    //
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("Post on space.");
    activity.setUserId(identitySpace.getId());
    cachedActivityStorage.saveActivity(identitySpace, activity);
    tearDownActivityList.add(activity);
    
    ExoSocialActivity userActivity = new ExoSocialActivityImpl();
    userActivity.setTitle("demo post on space");
    userActivity.setUserId(demo.getId());
    cachedActivityStorage.saveActivity(identitySpace, userActivity);
    tearDownActivityList.add(userActivity);
    
    List<ExoSocialActivity> spaceActivities = cachedActivityStorage.getSpaceActivities(identitySpace, 0, 10);
    
    for (ExoSocialActivity esa : spaceActivities) {
      assertEquals(esa.getStreamOwner(), space.getPrettyName());
    }
    
    String newDisplayName = "social team";
    
    cachedSpaceStorage.renameSpace(space, newDisplayName);
    Space got = cachedSpaceStorage.getSpaceById(space.getId());
    
    assertEquals(0, cacheService.getActivitiesCache().getCacheSize());
    
    spaceActivities = cachedActivityStorage.getSpaceActivities(identitySpace, 0, 10);
    
    for (ExoSocialActivity esa : spaceActivities) {
      assertEquals(esa.getStreamOwner(), got.getPrettyName());
    }
    
    cachedSpaceStorage.deleteSpace(space.getId());
  }
}
