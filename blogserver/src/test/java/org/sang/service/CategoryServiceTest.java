package org.sang.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sang.bean.Category;
import org.sang.mapper.CategoryMapper;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    // ========== getAllCategories tests ==========

    @Test
    void getAllCategories_shouldReturnAllCategories() {
        Category c1 = new Category();
        c1.setId(1L);
        c1.setCateName("Java");
        Category c2 = new Category();
        c2.setId(2L);
        c2.setCateName("Python");
        when(categoryMapper.getAllCategories()).thenReturn(Arrays.asList(c1, c2));

        List<Category> result = categoryService.getAllCategories();

        assertEquals(2, result.size());
        assertEquals("Java", result.get(0).getCateName());
        verify(categoryMapper).getAllCategories();
    }

    @Test
    void getAllCategories_shouldReturnEmptyList() {
        when(categoryMapper.getAllCategories()).thenReturn(Collections.emptyList());

        List<Category> result = categoryService.getAllCategories();

        assertTrue(result.isEmpty());
    }

    // ========== deleteCategoryByIds tests ==========

    @Test
    void deleteCategoryByIds_allDeleted_shouldReturnTrue() {
        when(categoryMapper.deleteCategoryByIds(any())).thenReturn(2);

        boolean result = categoryService.deleteCategoryByIds("1,2");

        assertTrue(result);
    }

    @Test
    void deleteCategoryByIds_partialDelete_shouldReturnFalse() {
        when(categoryMapper.deleteCategoryByIds(any())).thenReturn(1);

        boolean result = categoryService.deleteCategoryByIds("1,2,3");

        assertFalse(result);
    }

    @Test
    void deleteCategoryByIds_noneDeleted_shouldReturnFalse() {
        when(categoryMapper.deleteCategoryByIds(any())).thenReturn(0);

        boolean result = categoryService.deleteCategoryByIds("1,2");

        assertFalse(result);
    }

    @Test
    void deleteCategoryByIds_singleId() {
        when(categoryMapper.deleteCategoryByIds(any())).thenReturn(1);

        boolean result = categoryService.deleteCategoryByIds("1");

        assertTrue(result);
        verify(categoryMapper).deleteCategoryByIds(new String[]{"1"});
    }

    // ========== updateCategoryById tests ==========

    @Test
    void updateCategoryById_shouldDelegateToMapper() {
        Category category = new Category();
        category.setId(1L);
        category.setCateName("Updated");
        when(categoryMapper.updateCategoryById(category)).thenReturn(1);

        int result = categoryService.updateCategoryById(category);

        assertEquals(1, result);
        verify(categoryMapper).updateCategoryById(category);
    }

    @Test
    void updateCategoryById_notFound_shouldReturnZero() {
        Category category = new Category();
        category.setId(999L);
        when(categoryMapper.updateCategoryById(category)).thenReturn(0);

        int result = categoryService.updateCategoryById(category);

        assertEquals(0, result);
    }

    // ========== addCategory tests ==========

    @Test
    void addCategory_shouldSetDateAndDelegate() {
        Category category = new Category();
        category.setCateName("New Category");
        when(categoryMapper.addCategory(any(Category.class))).thenReturn(1);

        int result = categoryService.addCategory(category);

        assertEquals(1, result);
        assertNotNull(category.getDate());
        verify(categoryMapper).addCategory(category);
    }

    @Test
    void addCategory_shouldSetDateCloseToCurrentTime() {
        Category category = new Category();
        category.setCateName("Test");
        when(categoryMapper.addCategory(any(Category.class))).thenReturn(1);

        long before = System.currentTimeMillis();
        categoryService.addCategory(category);
        long after = System.currentTimeMillis();

        assertNotNull(category.getDate());
        long dateMs = category.getDate().getTime();
        assertTrue(dateMs >= before && dateMs <= after);
    }

    @Test
    void addCategory_failure_shouldReturnZero() {
        Category category = new Category();
        category.setCateName("Fail");
        when(categoryMapper.addCategory(any(Category.class))).thenReturn(0);

        int result = categoryService.addCategory(category);

        assertEquals(0, result);
    }
}
