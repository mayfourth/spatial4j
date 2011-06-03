package org.apache.lucene.spatial.base.prefix;

import org.apache.lucene.spatial.base.context.SpatialContext;
import org.apache.lucene.spatial.base.shape.BBox;
import org.apache.lucene.spatial.base.shape.Point;
import org.apache.lucene.spatial.base.shape.Shape;

import java.util.*;

/**
 * A SpatialPrefixGrid based on Geohashes.  Uses {@link GeohashUtils} to do all the geohash work.
 *
 * TODO at the moment it doesn't handle suffixes such as {@link SpatialPrefixGrid#INTERSECTS}. Only does full-resolution
 * points.
 */
public class GeohashSpatialPrefixGrid extends SpatialPrefixGrid {

  public GeohashSpatialPrefixGrid(SpatialContext shapeIO, int maxLevels) {
    super(shapeIO, maxLevels);
    int MAXP = getMaxLevelsPossible();
    if (maxLevels <= 0 || maxLevels > MAXP)
      throw new IllegalArgumentException("maxLen must be [1-"+MAXP+"] but got "+ maxLevels);
  }

  /** Any more than this and there's no point (double lat & lon are the same). */
  public static int getMaxLevelsPossible() { return GeohashUtils.MAX_PRECISION; }

  @Override
  public Collection<Cell> getCells(Shape shape) {
    BBox r = shape.getBoundingBox();
    double width = r.getMaxX() - r.getMinX();
    double height = r.getMaxY() - r.getMinY();
    int len = GeohashUtils.lookupHashLenForWidthHeight(width,height);
    len = Math.min(len,maxLevels-1);

    //TODO !! Bug: incomplete when at top level and covers more than 4 cells
    Set<Cell> cornerCells = new TreeSet<Cell>();
    cornerCells.add(getCell(r.getMinX(), r.getMinY(), len));
    cornerCells.add(getCell(r.getMinX(), r.getMaxY(), len));
    cornerCells.add(getCell(r.getMaxX(), r.getMaxY(), len));
    cornerCells.add(getCell(r.getMaxX(), r.getMinY(), len));

    return cornerCells;
  }

  @Override
  public Cell getCell(double x, double y, int level) {
    return new GhCell(GeohashUtils.encode(y, x, level));//args are lat,lon (y,x)
  }

  @Override
  public Cell getCell(String token) {
    return new GhCell(token);
  }

  @Override
  public Point getPoint(String token) {
    if (token.length() < maxLevels)
      return null;
    return GeohashUtils.decode(token,shapeIO);
  }

  /**
   * A cell in a geospatial grid hierarchy as specified by a {@link GeohashSpatialPrefixGrid}.
   */
  class GhCell extends SpatialPrefixGrid.Cell {
    public GhCell(String token) {
      super(token);
    }

    @Override
    public Collection<SpatialPrefixGrid.Cell> getSubCells() {
      if (getLevel() >= GeohashSpatialPrefixGrid.this.getMaxLevels())
        return null;
      String[] hashes = GeohashUtils.getSubGeoHashes(getGeohash());
      Arrays.sort(hashes);
      ArrayList<SpatialPrefixGrid.Cell> cells = new ArrayList<SpatialPrefixGrid.Cell>(hashes.length);
      for (String hash : hashes) {
        cells.add(new GhCell(hash));
      }
      return cells;
    }

    @Override
    public int getLevel() {
      return this.token.length();
    }

    @Override
    public BBox getShape() {
      return GeohashUtils.decodeBoundary(getGeohash(), shapeIO);// min-max lat, min-max lon
    }

    private String getGeohash() {
      return token;
//      if (getLevel() >= getMaxLevels())
//        return token.substring(0,token.length()-1);
//      else
//        return token;
    }

  }

}