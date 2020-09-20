package com.fc.mapper;

import com.fc.model.Collection;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CollectionMapper {

    List<Collection> listCollectionByCollectionId (List <Integer> idList);

    List<Collection> listCreatingCollectionByUserId(@Param("userId") Integer userId);

    void insertCollection(Collection collection);
}
