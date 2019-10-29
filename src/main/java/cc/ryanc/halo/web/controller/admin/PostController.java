package cc.ryanc.halo.web.controller.admin;

import cc.ryanc.halo.model.domain.Post;
import cc.ryanc.halo.model.domain.User;
import cc.ryanc.halo.model.dto.JsonResult;
import cc.ryanc.halo.model.dto.LogsRecord;
import cc.ryanc.halo.model.enums.BlogPropertiesEnum;
import cc.ryanc.halo.model.enums.PostStatusEnum;
import cc.ryanc.halo.model.enums.PostTypeEnum;
import cc.ryanc.halo.model.enums.ResultCodeEnum;
import cc.ryanc.halo.service.LogsService;
import cc.ryanc.halo.service.PostService;
import cc.ryanc.halo.utils.HaloUtils;
import cc.ryanc.halo.utils.LocaleMessageUtil;
import cc.ryanc.halo.utils.MarkdownUtils;
import cc.ryanc.halo.web.controller.core.BaseController;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.domain.Sort.Direction.DESC;

import static cc.ryanc.halo.model.dto.HaloConst.OPTIONS;
import static cc.ryanc.halo.model.dto.HaloConst.USER_SESSION_KEY;

/**
 * <pre>
 *     �?��?�文章管�?�控制器
 * </pre>
 *
 * @author : RYAN0UP
 * @date : 2017/12/10
 */
@Slf4j
@Controller
@RequestMapping(value = "/admin/posts")
public class PostController extends BaseController {

    @Autowired
    private PostService postService;

    @Autowired
    private LogsService logsService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private LocaleMessageUtil localeMessageUtil;

    /**
     * 去除html，htm�?�缀，以�?�将空格替�?��?-
     *
     * @param url url
     *
     * @return String
     */
    private static String urlFilter(String url) {
        if (null != url) {
            final boolean urlEndsWithHtmlPostFix = url.endsWith(".html") || url.endsWith(".htm");
            if (urlEndsWithHtmlPostFix) {
                return url.substring(0, url.lastIndexOf("."));
            }
        }
        return StrUtil.replace(url, " ", "-");
    }

    /**
     * 处�?��?��?�获�?�文章列表的请求
     *
     * @param model model
     * @return 模�?�路径admin/admin_post
     */
    @GetMapping
    public String posts(Model model,
                        @RequestParam(value = "status", defaultValue = "0") Integer status,
                        @PageableDefault(sort = "postDate", direction = DESC) Pageable pageable) {
        final Page<Post> posts = postService.findPostByStatus(status, PostTypeEnum.POST_TYPE_POST.getDesc(), pageable);
        model.addAttribute("posts", posts);
        model.addAttribute("publishCount", postService.getCountByStatus(PostStatusEnum.PUBLISHED.getCode()));
        model.addAttribute("draftCount", postService.getCountByStatus(PostStatusEnum.DRAFT.getCode()));
        model.addAttribute("trashCount", postService.getCountByStatus(PostStatusEnum.RECYCLE.getCode()));
        model.addAttribute("status", status);
        return "admin/admin_post";
    }

    /**
     * 模糊查询文章
     *
     * @param model   Model
     * @param keyword keyword 关键字
     * @return 模�?�路径admin/admin_post
     */
    @PostMapping(value = "/search")
    public String searchPost(Model model,
                             @RequestParam(value = "keyword") String keyword,
                             @PageableDefault(sort = "postId", direction = DESC) Pageable pageable) {
        try {
            Page<Post> posts = postService.searchPostsBy(keyword, PostTypeEnum.POST_TYPE_POST.getDesc(), PostStatusEnum.PUBLISHED.getCode(), pageable);
            model.addAttribute("posts", posts);
        } catch (Exception e) {
            log.error("未知错误：{}", e.getMessage());
        }
        return "admin/admin_post";
    }<<<<<<< MINE
=======


    /**
     * å¤„ç?†å?Žå?°èŽ·å?–æ–‡ç« åˆ—è¡¨çš„è¯·æ±‚
     *
     * @param model model
     * @param page  å½“å‰?é¡µç ?
     * @param size  æ¯?é¡µæ˜¾ç¤ºçš„æ?¡æ•°
     *
     * @return æ¨¡æ?¿è·¯å¾„admin/admin_post
     */
    
>>>>>>> YOURS
<<<<<<< MINE
=======


    /**
     * æ¨¡ç³ŠæŸ¥è¯¢æ–‡ç« 
     *
     * @param model   Model
     * @param keyword keyword å…³é�?®å­—
     * @param page    page å½“å‰?é¡µç ?
     * @param size    size æ¯?é¡µæ˜¾ç¤ºæ?¡æ•°
     *
     * @return æ¨¡æ?¿è·¯å¾„admin/admin_post
     */
    
>>>>>>> YOURS


    /**
     * 处�?�预览文章的请求
     *
     * @param postId 文章编�?�
     * @param model  model
     *
     * @return 模�?�路径/themes/{theme}/post
     */
    @GetMapping(value = "/view")
    public String viewPost(@RequestParam("postId") Long postId, Model model) {
        final Optional<Post> post = postService.findByPostId(postId);
        model.addAttribute("post", post.orElse(new Post()));
        return this.render("post");
    }

    /**
     * 处�?�跳转到新建文章页�?�
     *
     * @return 模�?�路径admin/admin_editor
     */
    @GetMapping(value = "/write")
    public String writePost() {
        return "admin/admin_post_new";
    }

    /**
     * 跳转到编辑文章页�?�
     *
     * @param postId 文章编�?�
     * @param model  model
     *
     * @return 模�?�路径admin/admin_editor
     */
    @GetMapping(value = "/edit")
    public String editPost(@RequestParam("postId") Long postId, Model model) {
        final Optional<Post> post = postService.findByPostId(postId);
        model.addAttribute("post", post.orElse(new Post()));
        return "admin/admin_post_edit";
    }

    /**
     * 添加文章
     *
     * @param post     post
     * @param cateList 分类列表
     * @param tagList  标签
     * @param session  session
     */
    @PostMapping(value = "/save")
    @ResponseBody
    public JsonResult save(@ModelAttribute Post post,
                           @RequestParam("cateList") List<String> cateList,
                           @RequestParam("tagList") String tagList,
                           HttpSession session) {
        final User user = (User) session.getAttribute(USER_SESSION_KEY);
        try {
            post.setPostContent(MarkdownUtils.renderMarkdown(post.getPostContentMd()));
            post.setUser(user);
            post = postService.buildCategoriesAndTags(post, cateList, tagList);
            post.setPostUrl(urlFilter(post.getPostUrl()));
            if (StrUtil.isNotEmpty(post.getPostPassword())) {
                post.setPostPassword(SecureUtil.md5(post.getPostPassword()));
            }
            //å½“æ²¡æœ‰é€‰æ‹©æ–‡ç« ç¼©ç•¥å›¾çš„æ—¶å€™ï¼Œè‡ªåŠ¨åˆ†é…?ä¸€å¼ å†…ç½®çš„ç¼©ç•¥å›¾
            if (StrUtil.equals(post.getPostThumbnail(), BlogPropertiesEnum.DEFAULT_THUMBNAIL.getProp())) {
                post.setPostThumbnail("/static/halo-frontend/images/thumbnail/thumbnail-" + RandomUtil.randomInt(1, 11) + ".jpg");
            }
            postService.save(post);
            logsService.save(LogsRecord.PUSH_POST, post.getPostTitle(), request);
            return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), localeMessageUtil.getMessage("code.admin.common.save-success"));
        } catch (Exception e) {
            log.error("Save article failed: {}", e.getMessage());
            e.printStackTrace();
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.common.save-failed"));
        }
    }

    /**
     * æ›´æ–°
     *
     * @param post     post
     * @param cateList åˆ†ç±»ç›®å½•
     * @param tagList  æ ‡ç­¾
<<<<<<< MINE
=======
     * @param session  session
     *
>>>>>>> YOURS
     * @return JsonResult
     */
    @PostMapping(value = "/update")
    @ResponseBody
    public JsonResult update(@ModelAttribute Post post,
                             @RequestParam("cateList") List<String> cateList,
                             @RequestParam("tagList") String tagList) {
        //old data
        final Post oldPost = postService.findByPostId(post.getPostId()).orElse(new Post());
        post.setPostViews(oldPost.getPostViews());
        post.setPostContent(MarkdownUtils.renderMarkdown(post.getPostContentMd()));
        post.setUser(oldPost.getUser());
        if (null == post.getPostDate()) {
            post.setPostDate(new Date());
        }
        post = postService.buildCategoriesAndTags(post, cateList, tagList);
        if (StrUtil.isNotEmpty(post.getPostPassword())) {
            post.setPostPassword(SecureUtil.md5(post.getPostPassword()));
        }
        //å½“æ²¡æœ‰é€‰æ‹©æ–‡ç« ç¼©ç•¥å›¾çš„æ—¶å€™ï¼Œè‡ªåŠ¨åˆ†é…?ä¸€å¼ å†…ç½®çš„ç¼©ç•¥å›¾
        if (StrUtil.equals(post.getPostThumbnail(), BlogPropertiesEnum.DEFAULT_THUMBNAIL.getProp())) {
            post.setPostThumbnail("/static/halo-frontend/images/thumbnail/thumbnail-" + RandomUtil.randomInt(1, 11) + ".jpg");
        }
        post = postService.save(post);
        if (null != post) {
            return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), localeMessageUtil.getMessage("code.admin.common.update-success"));
        } else {
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.common.update-failed"));
        }
    }

    /**
     * 处�?�移至回收站的请求
     *
     * @param postId 文章编�?�
     *
     * @return �?定�?�到/admin/posts
     */
    @GetMapping(value = "/throw")
    public String moveToTrash(@RequestParam("postId") Long postId, @RequestParam("status") Integer status) {
        try {
            postService.updatePostStatus(postId, PostStatusEnum.RECYCLE.getCode());
            log.info("Article number {} has been moved to the recycle bin", postId);
        } catch (Exception e) {
            log.error("Deleting article to recycle bin failed: {}", e.getMessage());
        }
        return "redirect:/admin/posts?status=" + status;
    }

    /**
     * 处�?�文章为�?�布的状�?
     *
     * @param postId 文章编�?�
     *
     * @return �?定�?�到/admin/posts
     */
    @GetMapping(value = "/revert")
    public String moveToPublish(@RequestParam("postId") Long postId,
                                @RequestParam("status") Integer status) {
        try {
            postService.updatePostStatus(postId, PostStatusEnum.PUBLISHED.getCode());
            log.info("Article number {} has been changed to release status", postId);
        } catch (Exception e) {
            log.error("Publishing article failed: {}", e.getMessage());
        }
        return "redirect:/admin/posts?status=" + status;
    }

    /**
     * 处�?�删除文章的请求
     *
     * @param postId 文章编�?�
     *
     * @return �?定�?�到/admin/posts
     */
    @GetMapping(value = "/remove")
    public String removePost(@RequestParam("postId") Long postId, @RequestParam("postType") String postType) {
        try {
            final Optional<Post> post = postService.findByPostId(postId);
            postService.remove(postId);
            logsService.save(LogsRecord.REMOVE_POST, post.get().getPostTitle(), request);
        } catch (Exception e) {
            log.error("Delete article failed: {}", e.getMessage());
        }
        if (StrUtil.equals(PostTypeEnum.POST_TYPE_POST.getDesc(), postType)) {
            return "redirect:/admin/posts?status=2";
        }
        return "redirect:/admin/page";
    }

    /**
     * 更新所有摘�?
     *
     * @param postSummary 文章摘�?字数
     *
     * @return JsonResult
     */
    @GetMapping(value = "/updateSummary")
    @ResponseBody
    public JsonResult updateSummary(@RequestParam("postSummary") Integer postSummary) {
        try {
            postService.updateAllSummary(postSummary);
        } catch (Exception e) {
            log.error("Update summary failed: {}", e.getMessage());
            e.printStackTrace();
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.common.update-failed"));
        }
        return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), localeMessageUtil.getMessage("code.admin.common.update-success"));
    }

    /**
     * 验�?文章路径是�?�已�?存在
     *
     * @param postUrl 文章路径
     *
     * @return JsonResult
     */
    @GetMapping(value = "/checkUrl")
    @ResponseBody
    public JsonResult checkUrlExists(@RequestParam("postUrl") String postUrl) {
        postUrl = urlFilter(postUrl);
        final Post post = postService.findByPostUrl(postUrl, PostTypeEnum.POST_TYPE_POST.getDesc());
        if (null != post) {
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.common.url-is-exists"));
        }
        return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), "");
    }

    /**
     * 将所有文章推�?到百度
     *
     * @param baiduToken baiduToken
     *
     * @return JsonResult
     */
    @GetMapping(value = "/pushAllToBaidu")
    @ResponseBody
    public JsonResult pushAllToBaidu(@RequestParam("baiduToken") String baiduToken) {
        if (StrUtil.isBlank(baiduToken)) {
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.post.no-baidu-token"));
        }
        final String blogUrl = OPTIONS.get(BlogPropertiesEnum.BLOG_URL.getProp());
        final List<Post> posts = postService.findAll(PostTypeEnum.POST_TYPE_POST.getDesc());
        final StringBuilder urls = new StringBuilder();
        for (Post post : posts) {
            urls.append(blogUrl);
            urls.append("/archives/");
            urls.append(post.getPostUrl());
            urls.append("\n");
        }
        final String result = HaloUtils.baiduPost(blogUrl, baiduToken, urls.toString());
        if (StrUtil.isEmpty(result)) {
            return new JsonResult(ResultCodeEnum.FAIL.getCode(), localeMessageUtil.getMessage("code.admin.post.push-to-baidu-failed"));
        }
        return new JsonResult(ResultCodeEnum.SUCCESS.getCode(), localeMessageUtil.getMessage("code.admin.post.push-to-baidu-success"));
    }

    @InitBinder
    public void initBinder(ServletRequestDataBinder binder) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
    }
}