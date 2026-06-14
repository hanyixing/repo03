package org.sang.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sang.bean.Article;
import org.sang.bean.Role;
import org.sang.bean.User;
import org.sang.mapper.ArticleMapper;
import org.sang.mapper.TagsMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private TagsMapper tagsMapper;

    @InjectMocks
    private ArticleService articleService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("testuser");
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(1L, "admin"));
        currentUser.setRoles(roles);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== addNewArticle tests ==========

    @Test
    void addNewArticle_newArticle_shouldGenerateSummary() {
        Article article = new Article();
        article.setId(-1L);
        article.setHtmlContent("<p>This is a test article content with some HTML</p>");
        article.setState(1);
        article.setSummary(null);

        when(articleMapper.addNewArticle(any(Article.class))).thenReturn(1);

        int result = articleService.addNewArticle(article);

        assertEquals(1, result);
        assertNotNull(article.getSummary());
        assertTrue(article.getSummary().length() <= 50);
        assertNotNull(article.getPublishDate());
        assertNotNull(article.getEditTime());
        assertEquals(1L, article.getUid());
        verify(articleMapper).addNewArticle(article);
    }

    @Test
    void addNewArticle_newArticle_withTags() {
        Article article = new Article();
        article.setId(-1L);
        article.setHtmlContent("content");
        article.setSummary("summary");
        article.setState(0);
        article.setDynamicTags(new String[]{"java", "spring"});

        when(articleMapper.addNewArticle(any(Article.class))).thenReturn(1);
        when(tagsMapper.deleteTagsByAid(any())).thenReturn(1);
        when(tagsMapper.saveTags(any())).thenReturn(1);
        when(tagsMapper.getTagsIdByTagName(any())).thenReturn(Arrays.asList(1L, 2L));
        when(tagsMapper.saveTags2ArticleTags(anyList(), anyLong())).thenReturn(2);

        int result = articleService.addNewArticle(article);

        assertEquals(1, result);
        assertNull(article.getPublishDate()); // state=0, no publish date
        verify(tagsMapper).deleteTagsByAid(any());
        verify(tagsMapper).saveTags(any());
        verify(tagsMapper).getTagsIdByTagName(any());
        verify(tagsMapper).saveTags2ArticleTags(anyList(), anyLong());
    }

    @Test
    void addNewArticle_newArticle_tagsFailure() {
        Article article = new Article();
        article.setId(-1L);
        article.setHtmlContent("content");
        article.setSummary("summary");
        article.setState(1);
        article.setDynamicTags(new String[]{"java", "spring"});

        when(articleMapper.addNewArticle(any(Article.class))).thenReturn(1);
        when(tagsMapper.deleteTagsByAid(any())).thenReturn(1);
        when(tagsMapper.saveTags(any())).thenReturn(1);
        when(tagsMapper.getTagsIdByTagName(any())).thenReturn(Arrays.asList(1L));
        when(tagsMapper.saveTags2ArticleTags(anyList(), anyLong())).thenReturn(1);

        int result = articleService.addNewArticle(article);

        assertEquals(-1, result);
    }

    @Test
    void addNewArticle_newArticle_shouldKeepProvidedSummary() {
        Article article = new Article();
        article.setId(-1L);
        article.setHtmlContent("<p>HTML content</p>");
        article.setSummary("My custom summary");
        article.setState(1);

        when(articleMapper.addNewArticle(any(Article.class))).thenReturn(1);

        articleService.addNewArticle(article);

        assertEquals("My custom summary", article.getSummary());
    }

    @Test
    void addNewArticle_updateExistingArticle() {
        Article article = new Article();
        article.setId(1L);
        article.setHtmlContent("content");
        article.setSummary("summary");
        article.setState(1);

        when(articleMapper.updateArticle(any(Article.class))).thenReturn(1);

        int result = articleService.addNewArticle(article);

        assertEquals(1, result);
        assertNotNull(article.getPublishDate());
        assertNotNull(article.getEditTime());
        verify(articleMapper).updateArticle(article);
        verify(articleMapper, never()).addNewArticle(any());
    }

    @Test
    void addNewArticle_updateExistingArticle_withTags() {
        Article article = new Article();
        article.setId(1L);
        article.setHtmlContent("content");
        article.setSummary("summary");
        article.setState(1);
        article.setDynamicTags(new String[]{"tag1"});

        when(articleMapper.updateArticle(any(Article.class))).thenReturn(1);
        when(tagsMapper.deleteTagsByAid(any())).thenReturn(1);
        when(tagsMapper.saveTags(any())).thenReturn(1);
        when(tagsMapper.getTagsIdByTagName(any())).thenReturn(Arrays.asList(1L));
        when(tagsMapper.saveTags2ArticleTags(anyList(), anyLong())).thenReturn(1);

        int result = articleService.addNewArticle(article);

        assertEquals(1, result);
        verify(tagsMapper).deleteTagsByAid(any());
    }

    // ========== getArticleById tests ==========

    @Test
    void getArticleById_shouldReturnArticleAndIncrementPv() {
        Article expected = new Article();
        expected.setId(1L);
        expected.setTitle("Test");
        when(articleMapper.getArticleById(1L)).thenReturn(expected);

        Article result = articleService.getArticleById(1L);

        assertEquals(expected, result);
        verify(articleMapper).pvIncrement(1L);
    }

    // ========== getArticleByState tests ==========

    @Test
    void getArticleByState_shouldCalculateStartOffset() {
        List<Article> articles = Collections.singletonList(new Article());
        when(articleMapper.getArticleByState(eq(1), eq(0), eq(10), eq(1L), eq("keyword")))
                .thenReturn(articles);

        List<Article> result = articleService.getArticleByState(1, 1, 10, "keyword");

        assertEquals(1, result.size());
        verify(articleMapper).getArticleByState(1, 0, 10, 1L, "keyword");
    }

    @Test
    void getArticleByState_withNullKeywords() {
        when(articleMapper.getArticleByState(eq(0), eq(10), eq(5), eq(1L), isNull()))
                .thenReturn(Collections.emptyList());

        List<Article> result = articleService.getArticleByState(0, 3, 5, null);

        assertTrue(result.isEmpty());
        verify(articleMapper).getArticleByState(0, 10, 5, 1L, null);
    }

    // ========== getArticleCountByState tests ==========

    @Test
    void getArticleCountByState_shouldDelegateToMapper() {
        when(articleMapper.getArticleCountByState(1, 1L, "test")).thenReturn(5);

        int count = articleService.getArticleCountByState(1, 1L, "test");

        assertEquals(5, count);
    }

    // ========== updateArticleState tests ==========

    @Test
    void updateArticleState_state2_shouldDelete() {
        Long[] aids = {1L, 2L};
        when(articleMapper.deleteArticleById(aids)).thenReturn(2);

        int result = articleService.updateArticleState(aids, 2);

        assertEquals(2, result);
        verify(articleMapper).deleteArticleById(aids);
        verify(articleMapper, never()).updateArticleState(any(), anyInt());
    }

    @Test
    void updateArticleState_stateNot2_shouldRecycle() {
        Long[] aids = {1L, 2L};
        when(articleMapper.updateArticleState(aids, 2)).thenReturn(2);

        int result = articleService.updateArticleState(aids, 0);

        assertEquals(2, result);
        verify(articleMapper).updateArticleState(aids, 2);
        verify(articleMapper, never()).deleteArticleById(any());
    }

    // ========== restoreArticle tests ==========

    @Test
    void restoreArticle_shouldSetStateToOne() {
        when(articleMapper.updateArticleStateById(1, 1)).thenReturn(1);

        int result = articleService.restoreArticle(1);

        assertEquals(1, result);
        verify(articleMapper).updateArticleStateById(1, 1);
    }

    // ========== pvStatisticsPerDay tests ==========

    @Test
    void pvStatisticsPerDay_shouldDelegateToMapper() {
        articleService.pvStatisticsPerDay();
        verify(articleMapper).pvStatisticsPerDay();
    }

    // ========== getCategories tests ==========

    @Test
    void getCategories_shouldReturnForCurrentUser() {
        List<String> expected = Arrays.asList("2024-01-01", "2024-01-02");
        when(articleMapper.getCategories(1L)).thenReturn(expected);

        List<String> result = articleService.getCategories();

        assertEquals(expected, result);
        verify(articleMapper).getCategories(1L);
    }

    // ========== getDataStatistics tests ==========

    @Test
    void getDataStatistics_shouldReturnForCurrentUser() {
        List<Integer> expected = Arrays.asList(10, 20, 30);
        when(articleMapper.getDataStatistics(1L)).thenReturn(expected);

        List<Integer> result = articleService.getDataStatistics();

        assertEquals(expected, result);
        verify(articleMapper).getDataStatistics(1L);
    }

    // ========== stripHtml tests ==========

    @Test
    void stripHtml_shouldRemoveAllHtmlTags() {
        String result = articleService.stripHtml("<div>Hello <span>World</span></div>");
        assertEquals("Hello World", result);
    }

    @Test
    void stripHtml_shouldRemovePTags() {
        String result = articleService.stripHtml("<p class='test'>Content</p>");
        assertEquals("Content", result);
    }

    @Test
    void stripHtml_shouldRemoveBrTags() {
        String result = articleService.stripHtml("Line1<br/>Line2<br />Line3");
        assertEquals("Line1Line2Line3", result);
    }

    @Test
    void stripHtml_shouldHandleEmptyString() {
        String result = articleService.stripHtml("");
        assertEquals("", result);
    }

    @Test
    void stripHtml_shouldHandlePlainText() {
        String result = articleService.stripHtml("No HTML here");
        assertEquals("No HTML here", result);
    }
}
