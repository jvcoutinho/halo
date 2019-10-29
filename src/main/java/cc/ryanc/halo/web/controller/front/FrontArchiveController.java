package cc.ryanc.halo.web.controller.front;

import cc.ryanc.halo.model.domain.Comment;
import cc.ryanc.halo.model.domain.Post;
import cc.ryanc.halo.model.domain.Tag;
import cc.ryanc.halo.model.dto.ListPage;
import cc.ryanc.halo.model.enums.*;
import cc.ryanc.halo.service.CommentService;
import cc.ryanc.halo.service.PostService;
import cc.ryanc.halo.utils.CommentUtil;
import cc.ryanc.halo.web.controller.core.BaseController;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

import static cc.ryanc.halo.model.dto.HaloConst.OPTIONS;

/**
 * <pre>
 *     �?�?�文章归档控制器
 * </pre>
 *
 * @author : RYAN0UP
 * @date : 2018/4/26
 */
@Slf4j
@Controller
@RequestMapping(value = "/archives")
public class FrontArchiveController extends BaseController {

    private static final String POSTS_CACHE_NAME = "posts";

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    /**
     * 文章归档
     *
     * @param model model
     *
     * @return 模�?�路径
     */
    @GetMapping
    public String archives(Model model) {
        return this.archives(model, 1, Sort.by(DESC, "postDate"));
    }

    /**
     * 文章归档分页
     *
     * @param model model
     * @param page  page 当�?页�?
     * @return 模�?�路径/themes/{theme}/archives
     */
    @GetMapping(value = "page/{page}")
    public String archives(Model model,
                           @PathVariable(value = "page") Integer page,
                           @SortDefault(sort = "postDate", direction = DESC) Sort sort) {
        //所有文章数�?�，分页，material主题适用
        final Pageable pageable = PageRequest.of(page - 1, 5, sort);
        final Page<Post> posts = postService.findPostByStatus(PostStatusEnum.PUBLISHED.getCode(), PostTypeEnum.POST_TYPE_POST.getDesc(), pageable);
        if (null == posts) {
            return this.renderNotFound();
        }
        model.addAttribute("is_archives", true);
        model.addAttribute("posts", posts);
        return this.render("archives");
    }<<<<<<< MINE
=======


    /**
     * æ–‡ç« å½’æ¡£åˆ†é¡µ
     *
     * @param model model
     * @param page  page å½“å‰?é¡µç ?
     *
     * @return æ¨¡æ?¿è·¯å¾„/themes/{theme}/archives
     */
    
>>>>>>> YOURS


    /**
     * 文章归档，根�?�年月
     *
     * @param model model
     * @param year  year 年份
     * @param month month 月份
     *
     * @return 模�?�路径/themes/{theme}/archives
     */
    @GetMapping(value = "{year}/{month}")
    public String archives(Model model,
                           @PathVariable(value = "year") String year,
                           @PathVariable(value = "month") String month) {
        final Page<Post> posts = postService.findPostByYearAndMonth(year, month, null);
        if (null == posts) {
            return this.renderNotFound();
        }
        model.addAttribute("is_archives", true);
        model.addAttribute("posts", posts);
        return this.render("archives");
    }

    /**
     * 渲染文章详情
     *
     * @param postUrl 文章路径�??
     * @param model   model
     *
     * @return 模�?�路径/themes/{theme}/post
     */
    @GetMapping(value = "{postUrl}")
    public String getPost(@PathVariable String postUrl,
                          @RequestParam(value = "cp", defaultValue = "1") Integer cp,
                          HttpServletRequest request,
                          Model model) {
        final Post post = postService.findByPostUrl(postUrl, PostTypeEnum.POST_TYPE_POST.getDesc());
        if (null == post || !post.getPostStatus().equals(PostStatusEnum.PUBLISHED.getCode())) {
            return this.renderNotFound();
        }
        //èŽ·å¾—å½“å‰?æ–‡ç« çš„å?‘å¸ƒæ—¥æœŸ
        final Date postDate = post.getPostDate();
        final Post prePost = postService.getPrePost(postDate);
        final Post nextPost = postService.getNextPost(postDate);
        if (null != prePost) {
            //å…¼å®¹è€?ç‰ˆæœ¬ä¸»é¢˜
            model.addAttribute("beforePost", prePost);
            model.addAttribute("prePost", prePost);
        }
        if (null != nextPost) {
            //å…¼å®¹è€?ç‰ˆæœ¬ä¸»é¢˜
            model.addAttribute("afterPost", nextPost);
            model.addAttribute("nextPost", nextPost);
        }
        List<Comment> comments = null;
        if (StrUtil.equals(OPTIONS.get(BlogPropertiesEnum.NEW_COMMENT_NEED_CHECK.getProp()), TrueFalseEnum.TRUE.getDesc()) || OPTIONS.get(BlogPropertiesEnum.NEW_COMMENT_NEED_CHECK.getProp()) == null) {
            comments = commentService.findCommentsByPostAndCommentStatus(post, CommentStatusEnum.PUBLISHED.getCode());
        } else {
            comments = commentService.findCommentsByPostAndCommentStatusNot(post, CommentStatusEnum.RECYCLE.getCode());
        }
        //èŽ·å?–æ–‡ç« çš„æ ‡ç­¾ç�?¨ä½œkeywords
        final List<Tag> tags = post.getTags();
        final List<String> tagWords = new ArrayList<>();
        if (tags != null) {
            for (Tag tag : tags) {
                tagWords.add(tag.getTagName());
            }
        }
        //é»˜è®¤æ˜¾ç¤º10æ?¡
        int size = 10;
        //èŽ·å?–æ¯?é¡µè¯„è®ºæ?¡æ•°
        if (StrUtil.isNotBlank(OPTIONS.get(BlogPropertiesEnum.INDEX_COMMENTS.getProp()))) {
            size = Integer.parseInt(OPTIONS.get(BlogPropertiesEnum.INDEX_COMMENTS.getProp()));
        }
        //è¯„è®ºåˆ†é¡µ
        final ListPage<Comment> commentsPage = new ListPage<Comment>(CommentUtil.getComments(comments), cp, size);
        final int[] rainbow = PageUtil.rainbow(cp, commentsPage.getTotalPage(), 3);
        model.addAttribute("is_post", true);
        model.addAttribute("comments", commentsPage);
        model.addAttribute("commentsCount", comments.size());
        model.addAttribute("rainbow", rainbow);
        model.addAttribute("tagWords", CollUtil.join(tagWords, ","));
        postService.cacheViews(post.getPostId());

        //åˆ¤æ–­æ–‡ç« æ˜¯å?¦æœ‰åŠ å¯†
        if (StrUtil.isNotEmpty(post.getPostPassword())) {
            Cookie cookie = ServletUtil.getCookie(request, "halo-post-password-" + post.getPostId());
            if (null == cookie) {
                post.setPostSummary("è¯¥æ–‡ç« ä¸ºåŠ å¯†æ–‡ç« ");
                post.setPostContent("<form id=\"postPasswordForm\" method=\"post\" action=\"/archives/verifyPostPassword\"><p>è¯¥æ–‡ç« ä¸ºåŠ å¯†æ–‡ç« ï¼Œè¾“å…¥æ­£ç¡®çš„å¯†ç ?å?³å?¯è®¿é—®ã€‚</p><input type=\"hidden\" id=\"postId\" name=\"postId\" value=\"" + post.getPostId() + "\"> <input type=\"password\" id=\"postPassword\" name=\"postPassword\"> <input type=\"submit\" id=\"passwordSubmit\" value=\"æ??äº¤\"></form>");
            }
        }
        model.addAttribute("post", post);
        return this.render("post");
    }

    /**
     * 验�?文章密�?
     *
     * @param postId       postId
     * @param postPassword postPassword
     * @param response     response
     *
     * @return String
     */
    @PostMapping(value = "/verifyPostPassword")
    @CacheEvict(value = POSTS_CACHE_NAME, allEntries = true, beforeInvocation = true)
    public String verifyPostPassword(@RequestParam(value = "postId") Long postId,
                                     @RequestParam(value = "postPassword") String postPassword,
                                     HttpServletResponse response) {
        final Post post = postService.findByPostId(postId, PostTypeEnum.POST_TYPE_POST.getDesc());
        if (null == post) {
            return this.renderNotFound();
        }
        if (SecureUtil.md5(postPassword).equals(post.getPostPassword())) {
            ServletUtil.addCookie(response, "halo-post-password-" + post.getPostId(), SecureUtil.md5(postPassword));
        }
        return "redirect:/archives/" + post.getPostUrl();
    }
}