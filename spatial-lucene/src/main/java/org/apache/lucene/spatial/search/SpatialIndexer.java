package org.apache.lucene.spatial.search;

import org.apache.lucene.document.Fieldable;
import org.apache.lucene.spatial.base.shape.Shape;

public interface SpatialIndexer<T extends SpatialFieldInfo> {

  Fieldable[] createFields(T indexInfo, Shape shape, boolean index, boolean store);
}