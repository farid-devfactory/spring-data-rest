package org.springframework.data.rest.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.repository.annotation.RestResource;
import org.springframework.util.StringUtils;

/**
 * Abstract class that contains the basic functionality that any exporter will need
 * to export a Repository implementation.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public abstract class RepositoryExporter<M extends RepositoryMetadata<E>, E extends EntityMetadata<? extends AttributeMetadata>>
    implements ApplicationContextAware,
               InitializingBean {

  protected ApplicationContext applicationContext;
  protected Repositories repositories;
  protected List<String> exportOnlyTheseClasses = Collections.emptyList();
  protected Map<String, M> repositoryMetadata;

  /**
   * Get the list of class names of Repositories to export.
   *
   * @return a List of class names to export
   */
  public List<String> getExportOnlyTheseClasses() {
    return exportOnlyTheseClasses;
  }

  /**
   * Set the class names of only those Repositories you want exported.
   * Default is to export all found Repositories.
   *
   * @param exportOnlyTheseClasses
   * @return @this
   */
  @SuppressWarnings({"unchecked"})
  public M setExportOnlyTheseClasses(List<String> exportOnlyTheseClasses) {
    this.exportOnlyTheseClasses = exportOnlyTheseClasses;
    return (M) this;
  }

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @SuppressWarnings({"unchecked"})
  @Override public void afterPropertiesSet() throws Exception {
    repositories = new Repositories(applicationContext);
    repositoryMetadata = new HashMap<String, M>();
    for (Class<?> domainType : repositories) {
      if (!exportOnlyTheseClasses.isEmpty() && !exportOnlyTheseClasses.contains(domainType.getName())) {
        // Don't export this domain type
        continue;
      }
      Class<?> repoClass = repositories.getRepositoryInformationFor(domainType).getRepositoryInterface();
      String name;
      RestResource pathSeg = repoClass.getAnnotation(RestResource.class);
      if (null != pathSeg) {
        name = pathSeg.path();
      } else {
        name = StringUtils.uncapitalize(repoClass.getSimpleName().replaceAll("Repository", ""));
      }
      repositoryMetadata.put(name, createRepositoryMetadata(name, domainType, repoClass, repositories));
    }
  }

  /**
   * Get the list of Repository names being exported.
   *
   * @return
   */
  public Set<String> repositoryNames() {
    return repositoryMetadata.keySet();
  }

  /**
   * Is a Repository being exporter that supports this domain type?
   *
   * @param domainType
   * @return {@literal true} if a Repository is being exported, {@literal false} otherwise.
   */
  public boolean hasRepositoryFor(Class<?> domainType) {
    for (M repoMeta : repositoryMetadata.values()) {
      if (repoMeta.domainType().isAssignableFrom(domainType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the RepositoryMetadata for the Repository responsible for this domain type.
   *
   * @param domainType
   * @return {@link RepositoryMetadata} instance
   */
  public M repositoryMetadataFor(Class<?> domainType) {
    for (M repoMeta : repositoryMetadata.values()) {
      if (repoMeta.domainType().isAssignableFrom(domainType)) {
        return repoMeta;
      }
    }
    return null;
  }

  /**
   * Get the {@link RepositoryMetadata} for the Repository exported under the given name.
   *
   * @param name
   * @return {@link RepositoryMetadata} instance
   */
  public M repositoryMetadataFor(String name) {
    return repositoryMetadata.get(name);
  }

  protected abstract M createRepositoryMetadata(String name,
                                                Class<?> domainType,
                                                Class<?> repoClass,
                                                Repositories repositories);

}
