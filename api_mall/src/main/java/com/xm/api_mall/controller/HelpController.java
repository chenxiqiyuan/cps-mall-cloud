package com.xm.api_mall.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xm.api_mall.mapper.SmHelpPageMapper;
import com.xm.api_mall.service.HelpService;
import com.xm.comment.annotation.LoginUser;
import com.xm.comment_serialize.module.mall.entity.SmHelpEntity;
import com.xm.comment_serialize.module.mall.entity.SmHelpPageEntity;
import com.xm.comment_serialize.module.mall.vo.HelpVo;
import com.xm.comment_utils.exception.GlobleException;
import com.xm.comment_utils.response.MsgEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.orderbyhelper.OrderByHelper;

import java.util.List;

@RestController
@RequestMapping("/help")
public class HelpController {

    @Autowired
    private HelpService helpService;
    @Autowired
    private SmHelpPageMapper smHelpPageMapper;

    @GetMapping
    public HelpVo help(@LoginUser Integer userId, String url){
        if(StrUtil.isBlank(url))
            throw new GlobleException(MsgEnum.PARAM_VALID_ERROR);
        SmHelpEntity smHelpEntity = helpService.getHelp(userId,url);
        HelpVo helpVo = new HelpVo();
        BeanUtil.copyProperties(smHelpEntity,helpVo);
        return helpVo;
    }

    @GetMapping("/page")
    public List<SmHelpPageEntity> getHelpPage(){
        OrderByHelper.orderBy("sort desc");
        return smHelpPageMapper.selectAll();
    }

}
