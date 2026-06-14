package org.sang.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sang.bean.Category;
import org.sang.mapper.CategoryMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CategoryService}.
 *
 * deleteCategoryByIds returns success only when every requested id was actually
 * deleted; getting that comparison wrong would silently break category/article
 * data relationships, so both the matching and mismatching cases are covered.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryMapper categoryMapper;

    @InjectMocks
    CategoryService categoryService;

    @Test
    void getAllCategories_delegatesToMapper() {
        List<Category> expected = new ArrayList<>();
        expected.add(new Category());
        when(categoryMapper.getAllCategories()).thenReturn(expected);

        List<Category> result = categoryService.getAllCategories();

        assertSame(expected, result);
        assertEquals(1, result.size());
    }

    @Test
    void deleteCategoryByIds_allRowsDeleted_returnsTrueAndSplitsIds() {
        when(categoryMapper.deleteCategoryByIds(any(String[].class))).thenReturn(3);

        boolean result = categoryService.deleteCategoryByIds("1,2,3");

        assertTrue(result);
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(categoryMapper).deleteCategoryByIds(captor.capture());
        assertArrayEquals(new String[]{"1", "2", "3"}, captor.getValue());
    }

    @Test
    void deleteCategoryByIds_fewerRowsDeletedThanRequested_returnsFalse() {
        // Three ids requested but only two rows affected -> partial failure.
        when(categoryMapper.deleteCategoryByIds(any(String[].class))).thenReturn(2);

        boolean result = categoryService.deleteCategoryByIds("1,2,3");

        assertFalse(result);
    }

    @Test
    void addCategory_stampsCreationDateBeforeInsert() {
        Category category = new Category();
        category.setCateName("Tech");
        assertNull(category.getDate());
        when(categoryMapper.addCategory(category)).thenReturn(1);

        int result = categoryService.addCategory(category);

        assertEquals(1, result);
        assertNotNull(category.getDate(), "creation date must be set before persisting");
        verify(categoryMapper).addCategory(category);
    }

    @Test
    void updateCategoryById_delegatesToMapper() {
        Category category = new Category();
        category.setId(9L);
        when(categoryMapper.updateCategoryById(category)).thenReturn(1);

        int result = categoryService.updateCategoryById(category);

        assertEquals(1, result);
        verify(categoryMapper).updateCategoryById(category);
    }
}
