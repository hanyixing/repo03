package org.sang.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sang.bean.Article;
import org.sang.bean.User;
import org.sang.mapper.ArticleMapper;
import org.sang.mapper.TagsMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ArticleService}.
 *
 * The article-publishing path (summary generation from HTML, tag association,
 * new vs. update branching) is the focus, since broken Markdown/HTML handling
 * here would corrupt published articles.
 */
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    ArticleMapper articleMapper;

    @Mock
    TagsMapper tagsMapper;

    @InjectMocks
    ArticleService articleService;

    private static final Long CURRENT_UID = 1L;

    @BeforeEach
    void setUpCurrentUser() {
        // ArticleService reads the logged-in user via Util.getCurrentUser(),
        // which pulls the principal from the SecurityContext. Populate a real
        // context instead of mocking the static accessor (Mockito 3.1 has no
        // static mocking support).
        User principal = new User();
        principal.setId(CURRENT_UID);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void stripHtml_removesParagraphBreakAndOtherTags() {
        String result = articleService.stripHtml(
                "<p class='intro'>Hello</p><br/>World<span>!</span>");
        assertEquals("HelloWorld!", result);
    }

    @Test
    void addNewArticle_newArticle_generatesSummaryAndPopulatesAuditFields() {
        StringBuilder body = new StringBuilder("<p>");
        for (int i = 0; i < 100; i++) {
            body.append('a');
        }
        body.append("</p>");

        Article article = new Article();
        article.setId(-1L);
        article.setState(1);
        article.setSummary(null);
        article.setHtmlContent(body.toString());

        when(articleMapper.addNewArticle(article)).thenReturn(1);

        int result = articleService.addNewArticle(article);

        assertEquals(1, result);
        assertEquals(50, article.getSummary().length(), "summary must be truncated to 50 chars");
        assertEquals(CURRENT_UID, article.getUid(), "uid must come from the current user");
        assertNotNull(article.getPublishDate(), "publish date must be set when state == 1");
        assertNotNull(article.getEditTime());
        verify(articleMapper).addNewArticle(article);
        verifyNoInteractions(tagsMapper);
    }

    @Test
    void addNewArticle_newArticleWithTags_persistsTagAssociations() {
        Article article = new Article();
        article.setId(-1L);
        article.setState(1);
        article.setSummary("manual summary");
        article.setDynamicTags(new String[]{"java", "spring"});

        when(articleMapper.addNewArticle(article)).thenReturn(1);
        when(tagsMapper.getTagsIdByTagName(any(String[].class)))
                .thenReturn(Arrays.asList(10L, 20L));
        when(tagsMapper.saveTags2ArticleTags(anyList(), eq(-1L))).thenReturn(2);

        int result = articleService.addNewArticle(article);

        assertEquals(1, result);
        verify(tagsMapper).deleteTagsByAid(-1L);
        verify(tagsMapper).saveTags(article.getDynamicTags());
        verify(tagsMapper).getTagsIdByTagName(any(String[].class));
        verify(tagsMapper).saveTags2ArticleTags(anyList(), eq(-1L));
    }

    @Test
    void addNewArticle_tagPersistenceMismatch_returnsMinusOne() {
        Article article = new Article();
        article.setId(-1L);
        article.setState(0);
        article.setSummary("manual summary");
        article.setDynamicTags(new String[]{"java", "spring"});

        when(articleMapper.addNewArticle(article)).thenReturn(1);
        when(tagsMapper.getTagsIdByTagName(any(String[].class)))
                .thenReturn(Arrays.asList(10L, 20L));
        // Only one association saved while two tags were supplied -> failure.
        when(tagsMapper.saveTags2ArticleTags(anyList(), eq(-1L))).thenReturn(1);

        int result = articleService.addNewArticle(article);

        assertEquals(-1, result, "mismatch between tags and saved associations must surface as -1");
    }

    @Test
    void addNewArticle_existingArticle_callsUpdateNotInsert() {
        Article article = new Article();
        article.setId(5L);
        article.setState(1);
        article.setSummary("manual summary");

        when(articleMapper.updateArticle(article)).thenReturn(1);

        int result = articleService.addNewArticle(article);

        assertEquals(1, result);
        assertNotNull(article.getPublishDate());
        assertNotNull(article.getEditTime());
        verify(articleMapper).updateArticle(article);
        verify(articleMapper, never()).addNewArticle(any());
    }

    @Test
    void getArticleByState_computesPaginationOffsetAndUsesCurrentUser() {
        List<Article> expected = new ArrayList<>();
        expected.add(new Article());
        // page 2, count 10 -> start offset 10; uid from current user.
        when(articleMapper.getArticleByState(1, 10, 10, CURRENT_UID, "kw"))
                .thenReturn(expected);

        List<Article> result = articleService.getArticleByState(1, 2, 10, "kw");

        assertSame(expected, result);
        verify(articleMapper).getArticleByState(1, 10, 10, CURRENT_UID, "kw");
    }

    @Test
    void updateArticleState_stateTwo_permanentlyDeletes() {
        Long[] aids = {1L, 2L};
        when(articleMapper.deleteArticleById(aids)).thenReturn(2);

        int result = articleService.updateArticleState(aids, 2);

        assertEquals(2, result);
        verify(articleMapper).deleteArticleById(aids);
        verify(articleMapper, never()).updateArticleState(any(), any());
    }

    @Test
    void updateArticleState_otherState_movesToRecycleBin() {
        Long[] aids = {1L};
        when(articleMapper.updateArticleState(aids, 2)).thenReturn(1);

        int result = articleService.updateArticleState(aids, 1);

        assertEquals(1, result);
        verify(articleMapper).updateArticleState(aids, 2);
        verify(articleMapper, never()).deleteArticleById(any());
    }

    @Test
    void getArticleById_returnsArticleAndIncrementsPageView() {
        Article expected = new Article();
        when(articleMapper.getArticleById(7L)).thenReturn(expected);

        Article result = articleService.getArticleById(7L);

        assertSame(expected, result);
        verify(articleMapper).pvIncrement(7L);
    }

    @Test
    void restoreArticle_setsStateToPublished() {
        when(articleMapper.updateArticleStateById(3, 1)).thenReturn(1);

        int result = articleService.restoreArticle(3);

        assertEquals(1, result);
        verify(articleMapper).updateArticleStateById(3, 1);
    }
}
