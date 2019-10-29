package cc.ryanc.halo.service.impl;

import cc.ryanc.halo.model.domain.Category;
import cc.ryanc.halo.model.domain.Post;
import cc.ryanc.halo.model.domain.Tag;
import cc.ryanc.halo.model.dto.Archive;
import cc.ryanc.halo.model.enums.BlogPropertiesEnum;
import cc.ryanc.halo.model.enums.PostStatusEnum;
import cc.ryanc.halo.model.enums.PostTypeEnum;
import cc.ryanc.halo.repository.PostRepository;
import cc.ryanc.halo.service.CategoryService;
import cc.ryanc.halo.service.PostService;
import cc.ryanc.halo.service.TagService;
import cc.ryanc.halo.utils.HaloUtils;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

import static org.springframework.data.jpa.domain.Specification.where;

import static cc.ryanc.halo.model.dto.HaloConst.OPTIONS;
import static cc.ryanc.halo.model.dto.HaloConst.POSTS_VIEWS;

/**
 * <pre>
 *     文章业务逻辑实现类
 * </pre>
 *
 * @author : RYAN0UP
 * @date : 2017/11/14
 */
@Service
public class PostServiceImpl implements PostService {

    private static final String POSTS_CACHE_NAME = "posts";

    private static final String COMMENTS_CACHE_NAME = "comments";

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    /**
     * �?存文章
     *
     * @param post Post
     *
     * @return Post
     */
    @Override
    @CacheEvict(value = {POSTS_CACHE_NAME, COMMENTS_CACHE_NAME}, allEntries = true, beforeInvocation = true)
    public Post save(Post post) {
        int postSummary = 50;
        if (StrUtil.isNotEmpty(OPTIONS.get(BlogPropertiesEnum.POST_SUMMARY.getProp()))) {
            postSummary = Integer.parseInt(OPTIONS.get(BlogPropertiesEnum.POST_SUMMARY.getProp()));
        }
        final String summaryText = StrUtil.cleanBlank(HtmlUtil.cleanHtmlTag(post.getPostContent()));
        if (summaryText.length() > postSummary) {
            final String summary = summaryText.substring(0, postSummary);
            post.setPostSummary(summary);
        } else {
            post.setPostSummary(summaryText);
        }
        return postRepository.save(post);
    }

    /**
     * 根�?�编�?�移除文章
     *
     * @param postId postId
     *
     * @return Post
     */
    @Override
    @CacheEvict(value = {POSTS_CACHE_NAME, COMMENTS_CACHE_NAME}, allEntries = true, beforeInvocation = true)
    public Post remove(Long postId) {
        final Optional<Post> post = this.findByPostId(postId);
        postRepository.delete(post.get());
        return post.get();
    }

    /**
     * 修改文章状�?
     *
     * @param postId postId
     * @param status status
     *
     * @return Post
     */
    @Override
    @CacheEvict(value = POSTS_CACHE_NAME, allEntries = true, beforeInvocation = true)
    public Post updatePostStatus(Long postId, Integer status) {
        final Optional<Post> post = this.findByPostId(postId);
        post.get().setPostStatus(status);
        return postRepository.save(post.get());
    }

    /**
     * 批�?更新文章摘�?
     *
     * @param postSummary postSummary
     */
    @Override
    @CacheEvict(value = POSTS_CACHE_NAME, allEntries = true, beforeInvocation = true)
    public void updateAllSummary(Integer postSummary) {
        final List<Post> posts = this.findAll(PostTypeEnum.POST_TYPE_POST.getDesc());
        for (Post post : posts) {
            String text = StrUtil.cleanBlank(HtmlUtil.cleanHtmlTag(post.getPostContent()));
            if (text.length() > postSummary) {
                post.setPostSummary(text.substring(0, postSummary));
            } else {
                post.setPostSummary(text);
            }
            postRepository.save(post);
        }
    }

    /**
     * 获�?�文章列表 �?分页
     *
     * @param postType post or page
     *
     * @return List
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'posts_type_'+#postType")
    public List<Post> findAll(String postType) {
        return postRepository.findPostsByPostType(postType);
    }

    /**
     * 模糊查询文章
     *
     * @param keyword    关键�?
     * @param postType   文章类型
     * @param postStatus 文章状�?
     * @param pageable   分页信�?�
     *
     * @return Page
     */
    @Override
    public Page<Post> searchPosts(String keyword, String postType, Integer postStatus, Pageable pageable) {
        return postRepository.findByPostTypeAndPostStatusAndPostTitleLikeOrPostTypeAndPostStatusAndPostContentLike(
                postType,
                postStatus,
                "%" + keyword + "%",
                postType,
                postStatus,
                "%" + keyword + "%",
                pageable
        ).map(post -> {
            if (StrUtil.isNotEmpty(post.getPostPassword())) {
                post.setPostSummary("è¯¥æ–‡ç« ä¸ºåŠ å¯†æ–‡ç« ");
            }
            return post;
        });
    }

    @Override
    public Page<Post> searchPostsBy(String keyword, String postType, Integer postStatus, Pageable pageable) {
        return postRepository.findAll(buildSearchSepcification(keyword, postType, postStatus), pageable)
                .map(post -> {
                    if (StrUtil.isNotEmpty(post.getPostPassword())) {
                        post.setPostSummary("该文章为加密文章");
                    }
                    return post;
                });
    }

    /**
     * 根�?�文章状�?查询 分页，用于�?��?�管�?�
     *
     * @param status   0，1，2
     * @param postType post or page
     * @param pageable 分页信�?�
     *
     * @return Page
     */
    @Override
    public Page<Post> findPostByStatus(Integer status, String postType, Pageable pageable) {
        return postRepository.findPostsByPostStatusAndPostType(status, postType, pageable).map(post -> {
            if (StrUtil.isNotEmpty(post.getPostPassword())) {
                post.setPostSummary("è¯¥æ–‡ç« ä¸ºåŠ å¯†æ–‡ç« ");
            }
            return post;
        });
    }

    /**
     * 根�?�文章状�?查询 分页，首页分页
     *
     * @param pageable pageable
     *
     * @return Page
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'posts_page_'+#pageable.pageNumber")
    public Page<Post> findPostByStatus(Pageable pageable) {
        return postRepository.findPostsByPostStatusAndPostType(PostStatusEnum.PUBLISHED.getCode(), PostTypeEnum.POST_TYPE_POST.getDesc(), pageable).map(post -> {
            if (StrUtil.isNotEmpty(post.getPostPassword())) {
                post.setPostSummary("è¯¥æ–‡ç« ä¸ºåŠ å¯†æ–‡ç« ");
            }
            return post;
        });
    }

    /**
     * 根�?�文章状�?查询
     *
     * @param status   0，1，2
     * @param postType post or page
     *
     * @return List
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'posts_status_type_'+#status+'_'+#postType")
    public List<Post> findPostByStatus(Integer status, String postType) {
        return postRepository.findPostsByPostStatusAndPostType(status, postType);
    }

    /**
     * 根�?�编�?�查询文章
     *
     * @param postId postId
     *
     * @return Optional
     */
    @Override
    public Optional<Post> findByPostId(Long postId) {
        return postRepository.findById(postId);
    }

    /**
     * 根�?�编�?�和类型查询文章
     *
     * @param postId postId
     *
     * @return Post
     */
    @Override
    public Post findByPostId(Long postId, String postType) {
        return postRepository.findPostByPostIdAndPostType(postId, postType);
    }

    /**
     * 根�?�文章路径查询
     *
     * @param postUrl  路径
     * @param postType post or page
     *
     * @return Post
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'posts_posturl_'+#postUrl+'_'+#postType")
    public Post findByPostUrl(String postUrl, String postType) {
        return postRepository.findPostByPostUrlAndPostType(postUrl, postType);
    }

    /**
     * 查询最新的5篇文章
     *
     * @return List
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'posts_latest'")
    public List<Post> findPostLatest() {
        return postRepository.findTopFive();
    }

    /**
     * 获�?�下一篇文章 较新
     *
     * @param postDate postDate
     *
     * @return Post
     */
    @Override
    public Post getNextPost(Date postDate) {
        return postRepository.queryNextPost(postDate);
    }

    /**
     * 获�?�下一篇文章 较�?
     *
     * @param postDate postDate
     *
     * @return Post
     */
    @Override
    public Post getPrePost(Date postDate) {
        return postRepository.queryPrePost(postDate);
    }

    /**
     * 查询归档信�?� 根�?�年份和月份
     *
     * @return List
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'archives_year_month'")
    public List<Archive> findPostGroupByYearAndMonth() {
        final List<Object[]> objects = postRepository.findPostGroupByYearAndMonth();
        final List<Archive> archives = new ArrayList<>();
        Archive archive = null;
        for (Object[] obj : objects) {
            archive = new Archive();
            archive.setYear(obj[0].toString());
            archive.setMonth(obj[1].toString());
            archive.setCount(obj[2].toString());
            archive.setPosts(this.findPostByYearAndMonth(obj[0].toString(), obj[1].toString()));
            archives.add(archive);
        }
        return archives;
    }

    /**
     * 查询归档信�?� 根�?�年份
     *
     * @return List
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'archives_year'")
    public List<Archive> findPostGroupByYear() {
        final List<Object[]> objects = postRepository.findPostGroupByYear();
        final List<Archive> archives = new ArrayList<>();
        Archive archive = null;
        for (Object[] obj : objects) {
            archive = new Archive();
            archive.setYear(obj[0].toString());
            archive.setCount(obj[1].toString());
            archive.setPosts(this.findPostByYear(obj[0].toString()));
            archives.add(archive);
        }
        return archives;
    }

    /**
     * @return List
     *
     * @Author Aquan
     * @Description 查询归档信�?� 返回所有文章
     * @Date 2019.1.4 11:16
     * @Param
     **/
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'archives_all'")
    public List<Archive> findAllPost() {
        final List<Post> posts = postRepository.findAllPost();
        final Integer count = postRepository.totalAllPostCount();
        final List<Archive> archives = new ArrayList<>();
        Archive archive = null;
        archive = new Archive();
        archive.setCount(String.valueOf(count));
        archive.setPosts(posts);
        archives.add(archive);

        return archives;
    }


    /**
     * 根�?�年份和月份查询文章
     *
     * @param year  year
     * @param month month
     *
     * @return List
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'posts_year_month_'+#year+'_'+#month")
    public List<Post> findPostByYearAndMonth(String year, String month) {
        return postRepository.findPostByYearAndMonth(year, month);
    }

    /**
     * 根�?�年份查询文章
     *
     * @param year year
     *
     * @return List
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'posts_year_'+#year")
    public List<Post> findPostByYear(String year) {
        return postRepository.findPostByYear(year);
    }

    /**
     * 根�?�年份和月份索引文章
     *
     * @param year     year year
     * @param month    month month
     * @param pageable pageable pageable
     *
     * @return Page
     */
    @Override
    public Page<Post> findPostByYearAndMonth(String year, String month, Pageable pageable) {
        return postRepository.findPostByYearAndMonth(year, month, null).map(post -> {
            if (StrUtil.isNotEmpty(post.getPostPassword())) {
                post.setPostSummary("è¯¥æ–‡ç« ä¸ºåŠ å¯†æ–‡ç« ");
            }
            return post;
        });
    }

    /**
     * æ ¹æ?®åˆ†ç±»ç›®å½•æŸ¥è¯¢æ–‡ç« 
     *
     * @param category category
     * @param pageable pageable
     *
     * @return Page
     */
    @Override
    @CachePut(value = POSTS_CACHE_NAME, key = "'posts_category_'+#category.cateId+'_'+#pageable.pageNumber")
    public Page<Post> findPostByCategories(Category category, Pageable pageable) {
        return postRepository.findPostByCategoriesAndPostStatus(category, PostStatusEnum.PUBLISHED.getCode(), pageable).map(post -> {
            if (StrUtil.isNotEmpty(post.getPostPassword())) {
                post.setPostSummary("è¯¥æ–‡ç« ä¸ºåŠ å¯†æ–‡ç« ");
            }
            return post;
        });
    }

    /**
     * æ ¹æ?®æ ‡ç­¾æŸ¥è¯¢æ–‡ç« ï¼Œåˆ†é¡µ
     *
     * @param tag      tag
     * @param pageable pageable
     *
     * @return Page
     */
    @Override
    @CachePut(value = POSTS_CACHE_NAME, key = "'posts_tag_'+#tag.tagId+'_'+#pageable.pageNumber")
    public Page<Post> findPostsByTags(Tag tag, Pageable pageable) {
        return postRepository.findPostsByTagsAndPostStatus(tag, PostStatusEnum.PUBLISHED.getCode(), pageable).map(post -> {
            if (StrUtil.isNotEmpty(post.getPostPassword())) {
                post.setPostSummary("è¯¥æ–‡ç« ä¸ºåŠ å¯†æ–‡ç« ");
            }
            return post;
        });
    }

    /**
     * 热门文章
     *
     * @return List
     */
    @Override
    @Cacheable(value = POSTS_CACHE_NAME, key = "'posts_hot'")
    public List<Post> hotPosts() {
        return postRepository.findPostsByPostTypeOrderByPostViewsDesc(PostTypeEnum.POST_TYPE_POST.getDesc());
    }

    /**
     * 当�?文章的相似文章
     *
     * @param post post
     *
     * @return List
     */
    @Override
    @CachePut(value = POSTS_CACHE_NAME, key = "'posts_related_'+#post.getPostId()")
    public List<Post> relatedPosts(Post post) {
        //èŽ·å?–å½“å‰?æ–‡ç« çš„æ‰€æœ‰æ ‡ç­¾
        final List<Tag> tags = post.getTags();
        final List<Post> tempPosts = new ArrayList<>();
        for (Tag tag : tags) {
            tempPosts.addAll(postRepository.findPostsByTags(tag));
        }
        //åŽ»æŽ‰å½“å‰?çš„æ–‡ç« 
        tempPosts.remove(post);
        //åŽ»æŽ‰é‡?å¤?çš„æ–‡ç« 
        final List<Post> allPosts = new ArrayList<>();
        for (int i = 0; i < tempPosts.size(); i++) {
            if (!allPosts.contains(tempPosts.get(i))) {
                allPosts.add(tempPosts.get(i));
            }
        }
        return allPosts;
    }

    /**
     * 获�?�所有文章的阅读�?
     *
     * @return Long
     */
    @Override
    public Long getPostViews() {
        return postRepository.getPostViewsSum();
    }

    /**
     * 根�?�文章状�?查询数�?
     *
     * @param status 文章状�?
     *
     * @return 文章数�?
     */
    @Override
    public Integer getCountByStatus(Integer status) {
        return postRepository.countAllByPostStatusAndPostType(status, PostTypeEnum.POST_TYPE_POST.getDesc());
    }

    /**
     * 缓存阅读数
     *
     * @param postId postId
     */
    @Override
    public void cacheViews(Long postId) {
        if (null != POSTS_VIEWS.get(postId)) {
            POSTS_VIEWS.put(postId, POSTS_VIEWS.get(postId) + 1);
        } else {
            POSTS_VIEWS.put(postId, 1L);
        }
    }

    /**
     * 组装分类目录和标签
     *
     * @param post     post
     * @param cateList cateList
     * @param tagList  tagList
     *
     * @return Post Post
     */
    @Override
    public Post buildCategoriesAndTags(Post post, List<String> cateList, String tagList) {
        final List<Category> categories = categoryService.strListToCateList(cateList);
        post.setCategories(categories);
        if (StrUtil.isNotEmpty(tagList)) {
            final List<Tag> tags = tagService.strListToTagList(StrUtil.trim(tagList));
            post.setTags(tags);
        }
        return post;
    }

    /**
     * 获�?�最近的文章
     *
     * @param limit �?�数
     *
     * @return List
     */
    @Override
    public List<Post> getRecentPosts(int limit) {
        return postRepository.getPostsByLimit(limit);
    }

    @NonNull
    private Specification<Post> buildSearchSepcification(@NonNull String keyword,
                                                         @NonNull String postType,
                                                         @NonNull Integer postStatus) {
        return Specification.where(postTitleLike(keyword)).or(postContentLike(keyword)).and(postTypeEqual(postType)).and(postStatusEqual(postStatus));
//        return (root, criteriaQuery, criteriaBuilder) -> {
//            List<Predicate> predicates = new LinkedList<>();
//
//            if (StringUtils.hasText(keyword)) {
//                predicates.add(criteriaBuilder.like(root.get("postContent"), keyword));
//                predicates.add(criteriaBuilder.or(criteriaBuilder.like(root.get("postTitle"), keyword)));
//            }
//
//            if (StringUtils.hasText(postType)) {
//                predicates.add(criteriaBuilder.equal(root.get("postType"), postType));
//            }
//
//            if (postStatus != null) {
//                predicates.add(criteriaBuilder.equal(root.get("postStatus"), postStatus));
//            }
//
//            return criteriaQuery.where(predicates.toArray(new Predicate[0])).getRestriction();
//        };
    }

    private Specification<Post> postContentLike(@NonNull String keyword) {
        Assert.hasText(keyword, "Keyword must not be blank");

        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("postContent")), "%" + keyword.toLowerCase() + "%");
    }

    private Specification<Post> postTitleLike(@NonNull String keyword) {
        Assert.hasText(keyword, "Keyword must not be blank");

        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("postTitle")), "%" + keyword.toLowerCase() + "%");
    }

    private Specification<Post> postTypeEqual(@NonNull String postType) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("postType"), postType);
    }

    private Specification<Post> postStatusEqual(@NonNull Integer postStatus) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("postStatus"), postStatus);
    }
}